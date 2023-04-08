package sk.db.relational.types;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import sk.utils.functional.F1;
import sk.utils.ids.IdUuid;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings({"unused"})
public class UTUuidIdToUuid implements UserType<Object>, ParameterizedType {
    public final static String type = "sk.db.relational.types.UTUuidIdToUuid";
    public static final String param = "targetType";

    private Class<?> idClass;
    private F1<UUID, Object> creator;

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty(param);
        try {
            idClass = Class.forName(enumClassName);
            Constructor<?> constructor = idClass.getConstructor(UUID.class);
            creator = uuid -> {
                if (uuid == null) {
                    return null;
                }
                try {
                    return constructor.newInstance(uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            };
        } catch (ClassNotFoundException e) {
            throw new HibernateException("Enum class not found ", e);
        } catch (NoSuchMethodException e) {
            throw new HibernateException("Class doesnt have uuid constructor ", e);
        }
    }

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class returnedClass() {
        return idClass;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        UUID uuid = (UUID) rs.getObject(names);
        return creator.apply(uuid);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            IdUuid ldt = (IdUuid) value;
            st.setObject(index, ldt.getId(), Types.OTHER);
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {
        return original;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        return cached;
    }
}
