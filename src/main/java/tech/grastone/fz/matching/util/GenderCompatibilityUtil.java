package tech.grastone.fz.matching.util;

import org.springframework.stereotype.Component;

import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.Orientation;

@Component
public class GenderCompatibilityUtil {

    public Gender requiredCandidateGender(UserDto user) {
        if (user == null || user.getGender() == null || user.getSexualOrientation() == null) {
            return null;
        }

        Gender gender = user.getGender();
        Orientation orientation = user.getSexualOrientation();

        if (orientation == Orientation.STRAIGHT) {
            return gender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
        }
        if (orientation == Orientation.GAY) {
            return Gender.MALE;
        }
        if (orientation == Orientation.LESBIAN) {
            return Gender.FEMALE;
        }
        return null;
    }

    public boolean isDiscoverable(UserDto viewer, UserDto candidate) {
        if (viewer == null || candidate == null) {
            return false;
        }

        Gender requiredGender = requiredCandidateGender(viewer);
        if (requiredGender != null && candidate.getGender() != requiredGender) {
            return false;
        }

        Gender reverseRequiredGender = requiredCandidateGender(candidate);
        return reverseRequiredGender == null || viewer.getGender() == reverseRequiredGender;
    }
}
