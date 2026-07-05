package wily.legacy.client;

import net.minecraft.client.Minecraft;
import wily.legacy.Legacy4J;
import wily.legacy.client.ContentManager.Category;
import wily.legacy.client.ContentManager.Pack;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ContentReinstaller {
    private ContentReinstaller() {
    }

    public record Result(int updated, int failed, boolean requiresResourceReload) {
    }

    private record Job(Category category, Pack pack) {
    }

    private static final class State {
        int updated;
        int failed;
        boolean requiresResourceReload;

        Result result() {
            return new Result(updated, failed, requiresResourceReload);
        }
    }

    public static CompletableFuture<Result> reinstallInstalledContent() {
        List<CompletableFuture<List<Job>>> futures = ContentManager.CATEGORIES.stream()
            .filter(category -> !ContentManager.STARTERPACKS_CATEGORY_ID.equals(category.id()))
            .map(ContentReinstaller::fetchJobs)
            .toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .thenCompose(v -> reinstall(jobs(futures)));
    }

    private static CompletableFuture<List<Job>> fetchJobs(Category category) {
        return ContentManager.fetchIndex(category).thenApply(packs -> packs.stream().map(pack -> new Job(category, pack)).toList());
    }

    private static List<Job> jobs(List<CompletableFuture<List<Job>>> futures) {
        return futures.stream().flatMap(future -> future.join().stream()).toList();
    }

    private static CompletableFuture<Result> reinstall(List<Job> jobs) {
        CompletableFuture<State> future = CompletableFuture.completedFuture(new State());
        for (Job job : jobs) {
            future = future.thenCompose(state -> reinstall(job, state));
        }
        return future.thenApply(State::result).thenApply(result -> {
            if (result.updated() == 0 && result.failed() == 0) {
                Legacy4J.LOGGER.info("Reinstall Content did not update any installed packs.");
            }
            return result;
        });
    }

    private static CompletableFuture<State> reinstall(Job job, State state) {
        Pack pack = job.pack();
        Category category = job.category();
        if (!ContentPackDownloader.needsUpdate(pack, category)) {
            return CompletableFuture.completedFuture(state);
        }
        return prepare(job).thenCompose(v -> ContentPackDownloader.download(pack, category)).handle((installed, throwable) -> {
            if (throwable != null) {
                state.failed++;
                Legacy4J.LOGGER.warn("Failed to reinstall content pack {}", pack.id(), throwable);
            } else if (ContentPackDownloader.isInstalled(pack, category)) {
                state.updated++;
                state.requiresResourceReload |= category.requiresResourceReload();
                Legacy4J.LOGGER.info("Reinstall Content updated {} ({}) from {}.", pack.name(), pack.id(), category.id());
            } else {
                state.failed++;
                Legacy4J.LOGGER.warn("Failed to reinstall content pack {}", pack.id());
            }
            return state;
        });
    }

    private static CompletableFuture<Void> prepare(Job job) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            try {
                ContentManager.prepareDownloadTarget(job.pack(), job.category());
                future.complete(null);
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
