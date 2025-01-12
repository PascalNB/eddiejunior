package com.thefatrat.eddiejunior;

import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.util.MetadataHolder;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class RequestManager {

    private final Map<String, Map<String, Object>> map;
    private final Random random;
    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;

    public RequestManager() {
        map = new TimedMap<>(Duration.ofHours(2).toMillis());
        random = new SecureRandom();
        encoder = Base64.getEncoder();
        decoder = Base64.getDecoder();
    }

    public String setupMetadata(MetadataHolder metadataHolder) {
        String id = populateHolder(metadataHolder);
        removeRequest(metadataHolder.getMetadataId());
        return id;
    }

    @NotNull
    public String createRequest(String name, Map<String, Object> metadata) {
        String key = storeMetadata(metadata);
        return encode(name, key);
    }

    public void removeRequest(String id) {
        String[] decoded = decode(id);
        if (decoded == null) {
            return;
        }
        map.remove(decoded[1]);
    }

    @Nullable
    private Map.Entry<String, Map<String, Object>> retrieveRequest(String id) throws BotErrorException {
        String[] decoded = decode(id);
        if (decoded == null) {
            return null;
        }
        Map<String, Object> metadata = getMetadataOrThrow(decoded[1]);
        return Map.entry(decoded[0], metadata);
    }

    @NotNull
    private String populateHolder(@NotNull MetadataHolder metadataHolder) {
        String id = metadataHolder.getMetadataId();
        Map.Entry<String, Map<String, Object>> request = retrieveRequest(id);
        if (request == null) {
            return id;
        }
        metadataHolder.setMetadata(request.getValue());
        return request.getKey();
    }

    @NotNull
    private String encode(@NotNull String name, @NotNull String key) {
        return '%' + encoder.encodeToString(name.getBytes()) + '-' + key;
    }

    @Nullable
    private String[] decode(@NotNull String id) {
        if (!id.startsWith("%")) {
            return null;
        }
        String[] split = id.substring(1).split("-", 2);
        split[0] = new String(decoder.decode(split[0]));
        return split;
    }

    @NotNull
    private String storeMetadata(@NotNull Map<String, Object> data) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String key = encoder.encodeToString(bytes);
        map.put(key, data);
        return key;
    }

    @Nullable
    public Map<String, Object> getMetadata(String key) {
        return map.get(key);
    }

    @NotNull
    public Map<String, Object> getMetadataOrThrow(String key) throws BotErrorException {
        Map<String, Object> object = getMetadata(key);
        if (object == null) {
            throw new BotErrorException("Request timed out");
        }
        return object;
    }

    public Modal.Builder createModal(String id, String title, Map<String, Object> metadata) {
        String newId = createRequest(id, metadata);
        return Modal.create(newId, title);
    }

    public StringSelectMenu.Builder createStringSelectMenu(String id, Map<String, Object> metadata) {
        String newId = createRequest(id, metadata);
        return StringSelectMenu.create(newId);
    }

}
