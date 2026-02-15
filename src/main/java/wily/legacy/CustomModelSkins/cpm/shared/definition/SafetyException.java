package wily.legacy.CustomModelSkins.cpm.shared.definition;

public class SafetyException extends RuntimeException {
    private BlockReason blockReason;

    public SafetyException(BlockReason blockReason) {
        super(blockReason.name());
        this.blockReason = blockReason;
    }

    private static final long serialVersionUID = 8223470990044738695L;

    public static enum BlockReason {BLOCK_LIST, LINK_OVERFLOW, CONFIG_DISABLED, TOO_MANY_CUBES, TEXTURE_OVERFLOW, UUID_LOCK,}

    public BlockReason getBlockReason() {
        return blockReason;
    }
}
