package jsk.progen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JpgModel {
    Map<JpgServiceId, JpgServiceDefinition> services;
}
