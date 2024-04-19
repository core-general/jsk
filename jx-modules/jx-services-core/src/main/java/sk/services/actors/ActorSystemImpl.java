package sk.services.actors;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jakarta.inject.Inject;
import lombok.Data;
import sk.services.async.IAsync;
import sk.services.ids.IIds;
import sk.services.rand.IRand;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.statics.Cc;
import sk.utils.tree.Tree;
import sk.utils.tree.TreePath;
import sk.utils.tuples.X;
import sk.utils.tuples.X3;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ActorSystemImpl implements ActorSystem {
    private @Inject IRand rand;
    private @Inject IIds ids;
    private @Inject IAsync async;
    private final List<ActorSystemExecutor> executors;
    private final Tree<String, PrivateActor> actorTree;
    private final Map<ActorId, PrivateActor> actors = new ConcurrentHashMap<>();
    private final Map<ActorId, ActorSystemExecutor> distinctThreadActors = new ConcurrentHashMap<>();


    @SuppressWarnings("unused")
    public ActorSystemImpl() {
        this(1);
    }

    @SuppressWarnings("WeakerAccess")
    public ActorSystemImpl(int threadBufferCount) {
        executors = Collections.unmodifiableList(IntStream.range(0, threadBufferCount)
                .mapToObj($ -> new ActorSystemExecutor($ + "", this::doProcessBufferedMessages))
                .collect(Collectors.toList()));
        actorTree = Tree.create();
    }

    private ActorSystemImpl(int threadBufferCount, IRand rand, IIds ids, IAsync async) {
        this(threadBufferCount);
        this.rand = rand;
        this.ids = ids;
        this.async = async;
    }

    @Override
    public synchronized ActorId newActor(O<ActorId> parent, O<String> id, boolean distinctThread,
            PublicActor processor) {
        ActorId aid = createActorId(parent, id);
        processor = prepareProcessor(processor);
        PrivateActor pa = new PrivateActor(aid, distinctThread, processor, new ReentrantLock());
        actorTree.setVal(aid.getPath(), pa);
        actors.put(aid, pa);
        if (distinctThread) {
            distinctThreadActors.put(aid, new ActorSystemExecutor(aid.getPath().toString(),
                    msg -> pa.getProcessor().processMessage(ActorSystemImpl.this, msg.i2(), msg.i1(), msg.i3())));
        }
        return aid;
    }

    @Override
    public synchronized void removeActor(ActorId actor) {
        doInLock(actor, () -> {
            actorTree
                    .getNode(actor.getPath()).get().processAll((x, y) -> y).stream()
                    .map($ -> $.getValue().get().getId())
                    .sorted(Comparator.comparing(x -> -x.getPath().getSize()))
                    .forEach(id -> doInLock(id, () -> actors.remove(id)));

            actorTree.removeNode(actor.getPath());
            actors.remove(actor);
        });
    }

    @Override
    public synchronized CompletableFuture<Boolean> stop(long timeoutMs) {
        List<CompletableFuture<Boolean>> all = Stream.concat(distinctThreadActors.values().stream(), executors.stream())
                .map($ -> $.stop(timeoutMs))
                .collect(Collectors.toList());

        F0<Boolean> booleanF0 = () -> async
                .supplyParallel(all.stream().map($ -> (F0<Boolean>) $::join).collect(Cc.toL()))
                .stream().allMatch($ -> $);
        return async.supplyBuf(booleanF0);
    }

    @Override
    public void send(ActorId from, ActorId to, Object message) {
        X3<ActorId, ActorId, Object> msg = X.x(from, to, message);
        if (actors.get(to).isDistinctThread()) {
            distinctThreadActors.get(to).sendMessageToActor(msg);
        } else {
            if (executors.size() == 1) {
                executors.get(0).sendMessageToActor(msg);
            } else {
                rand.rndFromList(executors).ifPresent($ -> $.sendMessageToActor(msg));
            }
        }
    }

    /**
     * Actor pre creator for any kind of preprocess like DI injection
     */
    @SuppressWarnings("WeakerAccess")
    protected PublicActor prepareProcessor(PublicActor processor) {
        return processor;
    }

    private ActorId createActorId(O<ActorId> parent, O<String> oid) {
        TreePath<String> parentPath = parent.map(ActorId::getPath).orElse(TreePath.emptyPath());
        String id = oid.orElseGet(() -> ids.shortIdS());
        TreePath<String> tp = parentPath.merge(TreePath.path(id));
        return new ActorId(tp);
    }

    private void doProcessBufferedMessages(X3<ActorId, ActorId, Object> msg) {
        if (executors.size() == 1) {
            actors.get(msg.i2()).getProcessor().processMessage(this, msg.i2(), msg.i1(), msg.i3());
        } else {
            doInLock(msg.i2(), () -> actors.get(msg.i2()).getProcessor().processMessage(this, msg.i2(), msg.i1(), msg.i3()));
        }
    }

    private void doInLock(ActorId id, R toDo) {
        PrivateActor actor = actors.get(id);
        if (actor != null) {
            try {
                actor.getLock().lock();
                if (actors.get(id) != null) {
                    toDo.run();
                }
            } finally {
                actor.getLock().unlock();
            }
        }
    }

    @Data
    private static class ActorSystemExecutor {
        final ForeverThreadWithFinish thread;
        final BlockingQueue<X3<ActorId, ActorId, Object>> processingQueue;

        ActorSystemExecutor(String id, C1<X3<ActorId, ActorId, Object>> processor) {
            this.processingQueue = new LinkedBlockingQueue<>();
            thread = new ForeverThreadWithFinish(() -> {
                try {
                    X3<ActorId, ActorId, Object> msg = processingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        processor.accept(msg);
                    }
                } catch (InterruptedException ignored) {}
            }, "ActorSystemExecutor-" + id, true);
            thread.start();
        }

        private void sendMessageToActor(X3<ActorId, ActorId, Object> msg) {
            processingQueue.add(msg);
        }

        private CompletableFuture<Boolean> stop(long msTimeout) {
            return thread.finishThread(msTimeout);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) {
        //RandImpl rand = new RandImpl();
        //IdsImpl ids = new IdsImpl(rand, new BytesImpl());
        //AsyncImpl async = new AsyncImpl();
        //
        //int actorCount = 100;
        //long messageCount = 100_000_000L;
        //int actorBufferThreadCount = 4;
        //boolean distinctActorThreads = false;
        //
        //List<String> results = Cc.fill(actorCount, "-1");
        //
        //ActorSystem as = new ActorSystemImpl(actorBufferThreadCount, rand, ids, async);
        //
        //@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        //ArrayBlockingQueue<String> bq = new ArrayBlockingQueue<>(1);
        //
        //PublicActor actorGenerator = (actorSystem, me, sender, message) -> {
        //    int i = Integer.parseInt(me.getPath().getLeaf());
        //    results.set(i, ids.shortIdS());
        //    if (((Long) message) == actorCount) {
        //        bq.add("");
        //    }
        //};
        //
        //List<ActorId> actors = IntStream
        //        .range(0, actorCount)
        //        .mapToObj($ -> as.newActor(O.empty(), O.of("" + $), distinctActorThreads, actorGenerator))
        //        .collect(Collectors.toList());
        //long msgId = 0L;
        //long t1 = System.currentTimeMillis();
        //for (int i = 0; i < messageCount; i++) {
        //    as.send(rand.rndFromList(actors).get(), rand.rndFromList(actors).get(), ++msgId);
        //}
        //try {
        //    bq.take();
        //    long t2 = System.currentTimeMillis();
        //    System.out.println((t2 - t1) / 1000f + " s");
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        //as.stop(Long.MAX_VALUE).thenRun(() -> System.out.println("Actor system finished!"));
    }
}
