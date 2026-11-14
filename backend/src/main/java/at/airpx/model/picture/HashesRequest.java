package at.airpx.model.picture;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HashesRequest(@NotEmpty @Size(max = 1000) List<@Pattern(regexp = "[a-fA-F0-9]{64}") String> hashes) {}
