```java
// Example DTO
public class SomeSensitiveDataDTO {
private String userPinfl;
private String userEmail;
private String internalNote;

    // Constructors, getters, setters...

    public SomeSensitiveDataDTO(String userPinfl, String userEmail, String internalNote) {
        this.userPinfl = userPinfl;
        this.userEmail = userEmail;
        this.internalNote = internalNote;
    }

    @Override
    public String toString() {
        return "SomeSensitiveDataDTO{" +
                "userPinfl='" + MaskingUtils.maskPinfl(userPinfl) + '\'' +
                ", userEmail='" + MaskingUtils.maskEmail(userEmail) + '\'' +
                ", internalNote='" + internalNote + '\'' + // Assuming internalNote doesn't need masking
                '}';
    }

    // Getters and setters
    public String getUserPinfl() { return userPinfl; }
    public void setUserPinfl(String userPinfl) { this.userPinfl = userPinfl; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
}
```