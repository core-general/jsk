package sk.test.land.testcontainers;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import lombok.Getter;
import org.testcontainers.containers.GenericContainer;
import sk.test.land.core.JskLand;

public abstract class JskLandContainer<DOCKER extends GenericContainer> extends JskLand {
    private volatile DOCKER container;
    @Getter protected final int outsidePort;

    public JskLandContainer(int outsidePort) {
        this.outsidePort = outsidePort;
    }

    protected abstract DOCKER createContainer(int port);

    public DOCKER getContainer() {
        return getStatusLock().getInLock(() -> {
            if (getStatus() == JskLandStatus.NOT_INITED) {
                try {
                    start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return container;
        });
    }

    @Override
    protected void doInit() throws Exception {
        container = createContainer(outsidePort);
        container.start();
    }

    @Override
    protected void doShutdown() throws Exception {
        container.stop();
    }
}
