package wily.legacy.util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public interface ModInfo {
    Collection<String> getAuthors();

    Optional<String> getHomepage();

    Optional<String> getIssues();

    Optional<String> getSources();
    Collection<String> getCredits();

    Collection<String> getLicense();

    String getDescription();

    Optional<String> getLogoFile(int i);

    Optional<Path> findResource(String s);

    String getId();

    String getVersion();

    String getName();
    default boolean isHidden(){
        return false;
    }
}
