package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.v2.model.Journey;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class JourneyProcessor {
    private final Cache cache;
    private Journey journey;

    public JourneyProcessor(@NonNull Cache cache,
                            @NonNull Journey journey) {
        this.cache = cache;
        this.journey = journey;
    }

    public void run() throws Exception {

    }
}
