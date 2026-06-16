package tech.grastone.fz.matching.dto;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockCheckResponseDto {
    private Set<Long> blockedUserIds = new LinkedHashSet<>();
}
