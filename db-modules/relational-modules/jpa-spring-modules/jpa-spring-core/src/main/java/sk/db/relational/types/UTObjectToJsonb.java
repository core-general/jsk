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
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import sk.services.json.IJson;
import sk.spring.services.ServiceLocator4SpringImpl;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

@SuppressWarnings({"WeakerAccess", "unused"})
public class UTObjectToJsonb implements UserType<Object>, ParameterizedType, UTWithContext, DynamicParameterizedType {
    public final static String type = "sk.db.relational.types.UTObjectToJsonb";
    public static final String param = "targetType";

    private Class<?> cls;
    private IJson json;

    public void setParameterValues(Properties parameters) {
        synchronized (this) {
            json = ServiceLocator4SpringImpl.instance.getService(IJson.class).get();
            cls = UtUtils.getType(parameters, param);
        }
    }

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class returnedClass() {
        return cls;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = rs.getString(names);
        return rs.wasNull() ? null : getJson().from(value, returnedClass());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setString(index, getJson().to(value));
        }
    }

    @Override
    public boolean isMutable() {
        return true;
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
        if (value == null) {return null;}
        return getJson().from(getJson().to(value), returnedClass());
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        if (value == null) {return null;}
        // Serialize the object into JSON so that the snapshot represents its state.
        return getJson().to(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        if (cached == null) {return null;}
        return getJson().from((String) cached, returnedClass());
    }

    IJson getJson() {
        return json;
    }
}
