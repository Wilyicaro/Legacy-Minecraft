package wily.legacy.Skins.client.compat;

public record ExternalSkinProviderCapabilities(boolean modPresent,
                                               boolean coreAvailable,
                                               boolean selectionAvailable,
                                               boolean packListingSupported,
                                               boolean packListingAvailable,
                                               boolean previewSupported,
                                               boolean previewAvailable,
                                               boolean previewPoseAvailable) {

    public boolean isDegraded() {
        if (!modPresent) return false;
        if (!coreAvailable) return true;
        if (!selectionAvailable) return true;
        if (packListingSupported && !packListingAvailable) return true;
        if (previewSupported && !previewAvailable) return true;
        return previewAvailable && !previewPoseAvailable;
    }

    public String summary() {
        return "present=" + modPresent
                + ", core=" + coreAvailable
                + ", selection=" + selectionAvailable
                + ", packListing=" + (packListingSupported ? packListingAvailable : "n/a")
                + ", preview=" + (previewSupported ? previewAvailable : "n/a")
                + ", previewPose=" + (previewSupported ? previewPoseAvailable : "n/a");
    }
}