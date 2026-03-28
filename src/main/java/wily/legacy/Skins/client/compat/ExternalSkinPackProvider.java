package wily.legacy.Skins.client.compat;

import java.util.List;

public interface ExternalSkinPackProvider extends ExternalSkinProvider {

    List<ExternalSkinPackDescriptor> loadPackDescriptors();
}
