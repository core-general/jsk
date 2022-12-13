---
-- #%L
-- Swiss Knife
-- %%
-- Copyright (C) 2019 - 2022 Core General
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
update gcl.gcl_job_group
set jg_status               = case
                                  when (jg_num_of_success_tasks + jg_num_of_fail_tasks - 1) = jg_num_of_overall_tasks then
                                      case
                                          when jg_num_of_fail_tasks > 0 then 'FAIL'
                                          else 'FINISH'
                                          end
                                  else jg_status
    end,
    jg_num_of_success_tasks = jg_num_of_success_tasks + 1,
    updated_at              = CURRENT_TIMESTAMP,
    version                 = version + 1
where jg_id = :jg_id
