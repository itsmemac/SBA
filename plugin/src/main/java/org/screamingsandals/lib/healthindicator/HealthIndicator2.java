/*
 * Copyright 2022 ScreamingSandals
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.screamingsandals.lib.healthindicator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.tasker.TaskerTime;
import org.screamingsandals.lib.visuals.DatableVisual;

public interface HealthIndicator2 extends DatableVisual<HealthIndicator2> {
    static HealthIndicator2 of() {
        return HealthIndicatorManager2.healthIndicator();
    }

    HealthIndicator2 showHealthInTabList(boolean flag);

    HealthIndicator2 symbol(Component symbol);

    HealthIndicator2 symbol(ComponentLike symbol);

    HealthIndicator2 startUpdateTask(long time, TaskerTime unit);

    HealthIndicator2 addTrackedPlayer(PlayerWrapper player);

    HealthIndicator2 removeTrackedPlayer(PlayerWrapper player);

    default HealthIndicator2 title(Component component) {
        return symbol(component);
    }

    default HealthIndicator2 title(ComponentLike component) {
        return symbol(component);
    }
}