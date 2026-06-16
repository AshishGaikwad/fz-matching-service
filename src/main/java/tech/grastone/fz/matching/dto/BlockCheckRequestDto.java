package tech.grastone.fz.matching.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockCheckRequestDto {
    private Long userId;
    private List<Long> candidateUserIds;
}
