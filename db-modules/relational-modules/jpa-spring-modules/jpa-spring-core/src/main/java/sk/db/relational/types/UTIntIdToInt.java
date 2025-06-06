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
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.ids.IdInt;
import sk.utils.statics.Ma;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

@SuppressWarnings({"unused"})
public class UTIntIdToInt implements UserType<Object>, ParameterizedType, DynamicParameterizedType, EnhancedUserType<Object> {
    public final static String type = "sk.db.relational.types.UTIntIdToInt";
    public static final String param = "targetType";

    private Class<?> idClass;
    private F1<Integer, Object> creator;

    @Override
    public void setParameterValues(Properties parameters) {
        try {
            idClass = UtUtils.getType(parameters, param);
            Constructor<?> constructor = idClass.getDeclaredConstructor(Integer.class);
            creator = inti -> {
                if (inti == null) {
                    return null;
                }
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(inti);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            };
        } catch (NoSuchMethodException e) {
            throw new HibernateException("Class doesnt have uuid constructor ", e);
        }
    }

    @Override
    public int getSqlType() {
        return Types.INTEGER;
    }

    @Override
    public Class returnedClass() {
        return idClass;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Integer uuid = (Integer) rs.getObject(names);
        return rs.wasNull() ? null : creator.apply(uuid);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        } else {
            IdInt ldt = (IdInt) value;
            st.setObject(index, ldt.getId(), Types.INTEGER);
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

    @Override
    public String toSqlLiteral(Object value) {
        return O.ofNull(value).map($ -> $.toString()).orElse(null);
    }

    @Override
    public String toString(Object value) throws HibernateException {
        return O.ofNull(value).map($ -> $.toString()).orElse(null);
    }

    @Override
    public Object fromStringValue(CharSequence sequence) throws HibernateException {
        return O.ofNull(sequence).map($ -> Ma.pi($.toString())).orElse(null);
    }
}
