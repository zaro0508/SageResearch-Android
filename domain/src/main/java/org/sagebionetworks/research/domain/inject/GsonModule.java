/*
 * BSD 3-Clause License
 *
 * Copyright 2018  Sage Bionetworks. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1.  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3.  Neither the name of the copyright holder(s) nor the names of any contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission. No license is granted to the trademarks of
 * the copyright holders even if such marks are included in this software.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sagebionetworks.research.domain.inject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.sagebionetworks.research.domain.RuntimeTypeAdapterFactory;
import org.sagebionetworks.research.domain.step.ActiveUIStepBase;
import org.sagebionetworks.research.domain.step.StepType;
import org.sagebionetworks.research.domain.step.ui.ActiveUIStep;
import org.threeten.bp.Duration;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dagger.multibindings.Multibinds;
import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public abstract class GsonModule {

    @MapKey
    public @interface ClassKey{
        Class value();
    }

    /**
     * @return The json Deserializer for an active step.
     */
    @Provides
    @IntoMap
    @ClassKey(ActiveUIStepBase.class)
    static JsonDeserializer provideActiveUIStep() {
        return new JsonDeserializer<ActiveUIStep>() {
            @Override
            public ActiveUIStep deserialize(final JsonElement json, final Type typeOfT,
                    final JsonDeserializationContext context)
                    throws JsonParseException {
                if (json.isJsonObject()) {
                    JsonObject object = json.getAsJsonObject();
                    String identifier = getStringFieldNonNull(object, "identifier");
                    String title = getStringFieldNullable(object, "title");
                    String text = getStringFieldNullable(object, "text");
                    String detail = getStringFieldNullable(object, "detail");
                    String footnote = getStringFieldNullable(object, "footnote");
                    Duration duration = null;
                    JsonElement durationElement = object.get("duration");
                    if (durationElement != null) {
                        if (!durationElement.isJsonPrimitive()) {
                            throw new JsonParseException("duration " + durationElement.toString() + " should be an integer");
                        }
                        int durationInSeconds = durationElement.getAsInt();
                        duration = Duration.ofSeconds(durationInSeconds);
                    }

                    return new ActiveUIStepBase(identifier, title, text, detail, footnote,
                            duration, false);
                }

                throw new JsonParseException("json " + json.toString() + "is not an object");
            }
        };
    }

    /**
     * Returns the string from the given field or null if the given field has been ommited from the JSON
     * @param json the json object to get the field from
     * @param key the name of the field to get from the json object
     * @return The string that corresponds to key or null if no such String exists
     */
    private static String getStringFieldNullable(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null) {
            return element.getAsString();
        }

        return null;
    }

    /**
     * Returns the string corresponding to the given key in the given json object, or throws a
     * JsonParseExecption if no such String exists.
     * @param json The json to get the string field from.
     * @param key The field to get as a string from the given json.
     * @return The string corresponding to the given key in the given json object.
     * @throws JsonParseException if there is no string corresponding to the given key in the json object.
     */
    private static String getStringFieldNonNull(JsonObject json, String key) throws JsonParseException {
        JsonElement element = json.get(key);
        if (element != null) {
            String result = element.getAsString();
            if (result != null) {
                return result;
            }
        }

        throw new JsonParseException("NonNull field "  + key + "of object " + json.toString() + "couldn't be parsed");
    }

    @Provides
    @Singleton
    static Gson provideGson(Map<Class, JsonDeserializer> jsonDeserializerMap,
        Set<RuntimeTypeAdapterFactory<?>> runtimeTypeAdapterFactories) {
        GsonBuilder builder = new GsonBuilder();
        for (Map.Entry<Class, JsonDeserializer> entry : jsonDeserializerMap.entrySet()) {
            builder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }
        for (RuntimeTypeAdapterFactory runtimeTypeAdapterFactory : runtimeTypeAdapterFactories) {
            builder.registerTypeAdapterFactory(runtimeTypeAdapterFactory);
        }

        return builder.create();
    }
}
