package app.unattach.model;

import java.util.Set;

public record ProcessEmailResult(String newUniqueId, Set<String> filenames) {}
