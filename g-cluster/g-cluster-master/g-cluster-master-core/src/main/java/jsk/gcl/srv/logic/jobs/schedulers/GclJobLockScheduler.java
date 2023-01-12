package jsk.gcl.srv.logic.jobs.schedulers;

/*-
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

import jsk.gcl.srv.logic.jobs.services.GclJobManager;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import jsk.gcl.srv.logic.scaling.storage.GclNodeStorage;
import jsk.gcl.srv.logic.scaling.workers.GclScalingLocalWorkerManager;
import lombok.extern.log4j.Log4j2;
import sk.exceptions.NotImplementedException;
import sk.services.boot.IBoot;
import sk.services.clusterworkers.kvonoff.CluKvBasedOnOffWorker;
import sk.services.clusterworkers.model.CluDelay;
import sk.utils.async.cancel.CancelGetter;

import javax.inject.Inject;

import static sk.utils.statics.Ti.second;

/**
 * 1. Takes jobs from DB (one by one for job groups) (locks them with state and life ping) and adds them to local queue
 * (according to the count of nodes in cluster and size of
 * local working pool)
 * 2. Checks all the current local jobs and updates their lifePing
 *
 * ------------------------------
 * ------------BUFFER:
 * 2 limits: lower limit activates request to DB, higher limit limits how much work we take from DB
 */
@Log4j2
public class GclJobLockScheduler extends CluKvBasedOnOffWorker<CluKvBasedOnOffWorker.IConf> implements IBoot {

    public static final int MIN_BUFFER_SIZE = 250;
    public static final int MAX_BUFFER_SIZE = 1000;

    public GclJobLockScheduler() {
        super("GclJobLockScheduler");
    }

    @Inject GclScalingLocalWorkerManager workManager;
    @Inject GclJobManager jobManager;
    @Inject GclJobStorage jobs;
    @Inject GclNodeStorage nodes;

    @Override
    public void run() {
        start(new CluKvBasedOnOffWorker.Conf(
                30 * second, CluDelay.fixed(5 * second),
                this::doTask,
                e -> log.error("", e)
        ));
    }

    private void doTask(CancelGetter cancelGetter) {
        try {
            tryLockNewTasks();
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * Алгоритм:
     * 1. Берем все данные о количестве тасок в очереди
     * 2. Если тасок в буфере меньше, чем min_buffer_size
     * 3. Считаем сколько нам нужно тасок добрать (смотрим сколько всего свободных тасок, смотрим сколько нод, прикидываем
     * сколько сейчас тасок на нодах и прикидываем сколько мы можем взять, чтобы было равномерно)
     * 4. Берем все job_groupы и по очереди из каждой начиная с самой старой пытаемся добрать тасок специальным запросом
     * (запрос предполагает, что мы апдейтим таски этой жоб групы со статусом реди и возвращаем список айди тех, кого
     * заапдейтили)
     * 5. Добавляем взятые таски в очередь и если не донабирали, переходим к следующей job_groupе
     *
     * todo упорядочить все таски по индексу даты создания и по нему их и забирать
     */
    private void tryLockNewTasks() {
        throw new NotImplementedException();//todo
        //int bufferedJobCount = jobManager.getBufferJobCount();
        //
        //if(bufferedJobCount>MIN_BUFFER_SIZE){
        //    return;
        //}
        //
        //long activeNodeCount = nodes.getActiveNodeCount();
        //
        //do {
        //    O<GclJobGroup> oJobGroup = jobs.getOldestNotFinishedJobGroup();
        //    if (oJobGroup.isEmpty()) {
        //        break;
        //    }
        //
        //    final GclJobGroup jobGroup = oJobGroup.get();
        //    final int numOfJobsLeft = jobGroup.getJgInnerState().getNumOfJobsFinished();
        //
        //    long numOfJobsToLock = Ma.clamp(jobGroup.getJgInnerState().getNumOfJobs() / activeNodeCount, 3, MAX_BUFFER_SIZE);
        //    List<GclJob> lockedJobs = bufferedJobCount = jobManager.getBufferJobCount();
        //} while (bufferedJobCount < MIN_BUFFER_SIZE);
    }
}
