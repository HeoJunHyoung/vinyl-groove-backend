package basic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String userId;
    private String username;
    private List<String> roles;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String userAgent;
}