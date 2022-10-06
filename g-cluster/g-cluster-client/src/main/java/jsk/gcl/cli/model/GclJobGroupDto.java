package jsk.gcl.cli.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GclJobGroupDto {
    String jobGroupId;//uuid or timed haiku is prefered
    List<GclJobDto<?, ?>> jobs;
}
