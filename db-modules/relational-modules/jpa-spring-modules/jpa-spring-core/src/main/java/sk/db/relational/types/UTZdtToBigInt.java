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
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@SuppressWarnings("unused")
public class UTZdtToBigInt implements UserType<Object>, UTWithContext {
    public final static String type = "sk.db.relational.types.UTZdtToBigInt";

    @Override
    public int getSqlType() {
        return Types.BIGINT;
    }

    @Override
    public Class returnedClass() {
        return ZonedDateTime.class;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        long timestamp = rs.getLong(names);
        return rs.wasNull() ? null : getInjector(session).times().toZDT(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.BIGINT);
        } else {
            ZonedDateTime zdt = (ZonedDateTime) value;
            st.setLong(index, getInjector(session).times().toMilli(zdt));
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

    private ZoneId getZone(SharedSessionContractImplementor session) {
        return getInjector(session).times().getZone();
    }
}
