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
import org.hibernate.type.EnumType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import sk.utils.functional.O;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

@SuppressWarnings({"unused", "removal"})
public class UTEnumToString implements UserType<Object>, ParameterizedType, DynamicParameterizedType, EnhancedUserType<Object> {
    public final static String type = "sk.db.relational.types.UTEnumToString";
    public static final String param = "enumClassName";
    private Class<Enum> enumClass;

    private EnumType enumType = new EnumType();

    {
        enumType.setTypeConfiguration(new TypeConfiguration());
    }

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty(param);
        try {
            if (enumClassName == null) {
                //auto mode
                enumType.setParameterValues(parameters);
                enumClass = enumType.returnedClass();
            } else {
                enumClass = (Class<Enum>) Class.forName(enumClassName);
            }
        } catch (ClassNotFoundException e) {
            throw new HibernateException("Enum class not found ", e);
        }
    }

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class returnedClass() {
        return enumClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String name = rs.getString(names);
        //noinspection unchecked
        return rs.wasNull() ? null : Enum.valueOf(returnedClass(), name);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, ((Enum) value).name());
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Enum) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Object fromXMLString(String xmlValue) {
        //noinspection unchecked
        return Enum.valueOf(enumClass, xmlValue);
    }

    public String objectToSQLString(Object value) {
        return '\'' + ((Enum) value).name() + '\'';
    }

    public String toXMLString(Object value) {
        return ((Enum) value).name();
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
        return O.ofNull(sequence).map($ -> Enum.valueOf(enumClass, ($.toString()))).orElse(null);
    }
}
