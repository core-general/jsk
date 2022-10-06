package sk.si;/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.statics.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class JxSystemInfoService {
    private final SystemInfo si = new SystemInfo();

    public JxProcessorLoadData getProcessorLoad(long delay) {
        final double[] coreInfo = si.getHardware().getProcessor().getProcessorCpuLoad(delay);

        final double avg = Ar.avg(coreInfo);
        final double sko = Math.sqrt(
                DoubleStream.of(coreInfo).map($ -> Math.pow($ - avg, 2)).sum()//dispersion
        );

        return new JxProcessorLoadData(avg, coreInfo, Ar.sortCopy(coreInfo), sko);
    }

    public JxGeneralRAMData getGeneralMemoryInfo() {
        final GlobalMemory memory = si.getHardware().getMemory();
        return new JxGeneralRAMData(memory.getAvailable(), memory.getTotal());
    }

    public JxJavaProcessMemoryUsage getProcessMemoryUsed() {
        final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
        final MemoryUsage heapMemoryUsage = mxBean.getHeapMemoryUsage();
        final MemoryUsage nonHeap = mxBean.getNonHeapMemoryUsage();
        return new JxJavaProcessMemoryUsage(
                heapMemoryUsage.getUsed(),
                nonHeap.getUsed(),
                heapMemoryUsage.getMax() == -1 ? heapMemoryUsage.getCommitted() : heapMemoryUsage.getMax(),
                nonHeap.getMax() == -1 ? nonHeap.getCommitted() : nonHeap.getMax()
        );
    }

    /*
    import oshi.SystemInfo;
    import oshi.hardware.GlobalMemory;
    import sk.utils.async.ForeverThreadWithFinish;
    import sk.utils.statics.*;

    import java.util.List;
    import java.util.concurrent.CompletableFuture;
    import java.util.stream.DoubleStream;
    import java.util.stream.IntStream;
     */
    public static JxSystemInfoService ssi = new JxSystemInfoService();
    public static List<LoadThread> ourLoad = Cc.l();

    public static void main(String[] args) {
        new MonitorThread().start();
        Io.endlessReadFromKeyboard("XXX", s -> {
            synchronized (ssi) {
                if (Ma.isInt(s)) {
                    final int numOfThreads = Ma.pi(s);
                    restartLoad(numOfThreads);
                }
            }
        });
    }

    private static void restartLoad(int numOfThreads) {
        CompletableFuture.allOf(ourLoad.stream().map($ -> $.finishThread()).toArray(CompletableFuture[]::new)).join();
        ourLoad = IntStream.range(0, numOfThreads).mapToObj($ -> {
            final LoadThread loadThread = new LoadThread();
            loadThread.start();
            return loadThread;
        }).toList();
    }

    private static class LoadThread extends ForeverThreadWithFinish {
        public LoadThread() {
            super((ct) -> {
                int k = 0;
                for (int i = 0; i < 2_000_000_000; i++) {
                    if (ct.isCancelled()) {break;}
                    k = k + i * k + i * i;
                    if (i % 100_000_000 == 0) {
                        Ti.sleep(100);
                    }
                }
            }, true);
        }
    }

    private static class MonitorThread extends ForeverThreadWithFinish {
        public MonitorThread() {
            super((ct) -> {
                System.out.println(ssi.getProcessorLoad(5000));
                System.out.println();
                System.out.println(ssi.getGeneralMemoryInfo());
                System.out.println(ssi.getProcessMemoryUsed());
            }, true);
        }
    }
}
