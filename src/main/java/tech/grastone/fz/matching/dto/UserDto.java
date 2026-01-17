package tech.grastone.fz.matching.dto;


import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.grastone.fz.matching.enums.Gender;
import tech.grastone.fz.matching.enums.Orientation;
import tech.grastone.fz.matching.enums.SubscriptionPlan;
import tech.grastone.fz.matching.enums.UserStatus;

@Getter
@Setter
@ToString
public class UserDto {
	private long id;
	private String fullName;
    private String email;
    private String mobile;
    private LocalDate dob;
    private Gender gender;
    private Orientation sexualOrientation;
    private String password;
    private String bio;
    private UserStatus status;
    private String profilePicUrl;
    private double lattitude;
    private double longitude;
    private SubscriptionPlan subscriptionPlan;
    private LocalDate planExpiryDate;
}
