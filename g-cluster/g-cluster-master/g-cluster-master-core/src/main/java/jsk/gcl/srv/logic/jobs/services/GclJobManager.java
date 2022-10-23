package jsk.gcl.srv.logic.jobs.services;

import jsk.gcl.cli.model.GclJobDto;
import jsk.gcl.cli.model.GclJobId;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import sk.utils.async.locks.JReadWriteLock;
import sk.utils.async.locks.JReadWriteLockDecorator;
import sk.utils.statics.Cc;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GclJobManager {
    private ArrayBlockingQueue<GclJobDto<?, ?>> freeJobs;
    private ConcurrentHashMap<GclJobId, GclJobDto<?, ?>> inProgressJobs;

    private final JReadWriteLock rwl = new JReadWriteLockDecorator(new ReentrantReadWriteLock(true));

    @Inject GclJobStorage jobs;

    @PostConstruct
    public void init() {
        rwl.writeLock().runInLock(() -> {
            if (freeJobs == null) {
                freeJobs = new ArrayBlockingQueue<>(1000, true);
                inProgressJobs = new ConcurrentHashMap<>();
            }
        });
    }

    public List<GclJobId> getAllNodeJobs() {
        return rwl.readLock().getInLock(() -> {
            return Cc.addStream(freeJobs.stream().map($ -> $.getJobId()), inProgressJobs.keySet().stream()).distinct().toList();
        });
    }


    public GclJobDto<?, ?> acquireJobBlocking() {
        return rwl.writeLock().getInLock(() -> {
            try {
                final GclJobDto<?, ?> job = freeJobs.take();
                inProgressJobs.put(job.getJobId(), job);
                return job;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void finishJob(GclJobId id) {
        rwl.writeLock().runInLock(() -> {
            inProgressJobs.remove(id);
        });
    }

    public void addJobs(List<GclJobDto<?, ?>> jobs) {
        rwl.writeLock().runInLock(() -> {
            freeJobs.addAll(jobs);
        });
    }
}
