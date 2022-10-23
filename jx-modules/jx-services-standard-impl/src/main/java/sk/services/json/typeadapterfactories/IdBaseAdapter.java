package sk.services.json.typeadapterfactories;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;
import sk.utils.ids.IdBase;
import sk.utils.statics.Re;

import java.io.IOException;
import java.lang.reflect.Constructor;

public abstract class IdBaseAdapter<A extends Comparable<A>, T extends IdBase<A>> extends TypeAdapter<T> {
    final Constructor<T> constructor;

    public IdBaseAdapter() {
        Class<T> idBaseCls = (Class<T>) Re.getParentParameters(this.getClass()).get()[0];
        Class<A> parameterCls = (Class<A>) Re.getParentParameters(this.getClass()).get()[1];

        constructor = idBaseCls.getConstructor(parameterCls);
        constructor.setAccessible(true);
    }

    protected abstract A construct(String value);

    @Override
    public final void write(JsonWriter out, T value) throws IOException {
        if (value != null) {
            out.value(value.getId().toString());
        } else {
            out.value((String) null);
        }
    }

    @SneakyThrows
    @Override
    public final T read(JsonReader in) throws IOException {
        if (in.nextString() == null) {
            return null;
        }

        final A created = construct(in.nextString());
        return constructor.newInstance(created);
    }
}
