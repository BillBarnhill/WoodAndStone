/*
 * Copyright 2014 MovingBlocks
 *
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
 */
package org.terasology.workstation.system;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.WorkstationProcess;

import java.util.LinkedList;
import java.util.List;

public class DefaultWorkstationProcess implements WorkstationProcess {
    private String id;
    private List<ProcessPart> processParts = new LinkedList<>();

    public DefaultWorkstationProcess(Prefab prefab) {
        id = "Prefab:" + prefab.getURI().toSimpleString();
        for (Component component : prefab.iterateComponents()) {
            if (component instanceof ProcessPart) {
                processParts.add((ProcessPart) component);
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<ProcessPart> getProcessParts() {
        return processParts;
    }
}
