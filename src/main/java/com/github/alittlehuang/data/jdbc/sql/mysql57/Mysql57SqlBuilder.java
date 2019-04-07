package com.github.alittlehuang.data.jdbc.sql.mysql57;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.query.specification.Criteria;
import com.github.alittlehuang.data.query.specification.Expression;
import com.github.alittlehuang.data.query.specification.Selection;
import com.github.alittlehuang.data.query.specification.WhereClause;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Mysql57SqlBuilder implements SqlBuilder {

    @Override
    public SqlBuilder.PrecompiledSql count(Criteria<?> query) {
        return null;
    }

    @Override
    public SqlBuilder.PrecompiledSql exists(Criteria<?> query) {
        return null;
    }

    @Override
    public SqlBuilder.PrecompiledSql listResult(Criteria<?> query) {
        return null;
    }

    @Override
    public SqlBuilder.PrecompiledSql ListObjects(Criteria<?> query) {
        List<? extends Selection<?>> selections = query.getSelections();
        if ( selections == null || selections.isEmpty() ) {
            throw new RuntimeException("the selections must not be empty");
        }
        return null;
    }

    private static class Buidler<T> {

        private Criteria<?> criteria;
        EntityInformation<T, ?> entityInfo;
        WhereClause<T> whereClause;

        StringBuilder sqlWhere = new StringBuilder();

        public Buidler(Criteria<T> criteria) {
            this.criteria = criteria;
            entityInfo = EntityInformation.getInstance(criteria.getJavaType());
            whereClause = criteria.getWhereClause();
        }


        private static final Map<Class, String> SELECT_FROM_CLASS_SQL = new ConcurrentHashMap<>();

        private String selectFrom() {
            Class<?> clazz = criteria.getJavaType();
            return SELECT_FROM_CLASS_SQL.computeIfAbsent(clazz, javaType -> {
                StringBuilder builder = new StringBuilder("\nSELECT ");
                EntityInformation<?, ?> entityInformation = EntityInformation.getInstance(clazz);

                boolean first = true;
                for ( Attribute<?, ?> attribute : entityInformation.getAllAttributes() ) {
                    if ( first ) {
                        builder.append("\n  ");
                        first = false;
                    } else {
                        builder.append(",\n  ");
                    }
                    builder.append("`").append(attribute.getColumnName()).append("`");
                }
                builder.append("\nFROM ")
                        .append("\n  `")
                        .append(entityInformation.getTableName())
                        .append("`")
                        .append("\n");

                return builder.toString();
            });

        }

        private void appendWhere(WhereClause<T> whereClause) {
            if ( whereClause.isCompound() ) {
                recursiveBuild(whereClause);
            } else {
                build(whereClause);
            }
        }

        private void build(WhereClause<T> whereClause) {
            Expression<T> expression = whereClause.getExpression();
            Expression.Function function = expression.getFunction();
            if ( function != Expression.Function.NONE ) {
                // TODO
                throw new UnsupportedOperationException();
            }

            String[] names = expression.getNames(entityInfo.getJavaType());
            if ( names.length != 1 ) {
                // TODO
                throw new UnsupportedOperationException();
            }

            String columnName = entityInfo.getAttribute(names[0]).getColumnName();

            switch ( whereClause.getConditionalOperator() ) {
                case EQUAL:
                    break;
                case GREATER_THAN:
                    break;
                case LESS_THAN:
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    break;
                case BETWEEN:
                    break;
                case IN:
                    break;
                case LIKE:
                    break;
                case IS_NULL:
                    break;
            }

        }

        private void recursiveBuild(WhereClause<T> whereClause) {

            int appendIndex = sqlWhere.length();

            List<? extends WhereClause<T>> items = whereClause.getCompoundItems();
            if ( items.size() == 1 ) {
                build(items.get(0));
            } else if ( !items.isEmpty() ) {
                boolean fist = true;
                for ( WhereClause<T> item : items ) {
                    if ( false ) {
                        fist = false;
                    } else {
                        Predicate.BooleanOperator operator = item.getBooleanOperator();
                        sqlWhere.append(operator == Predicate.BooleanOperator.OR ? " OR " : " AND ");
                    }
                    boolean compound = item.isCompound() && item.getCompoundItems().size() > 1;
                    if ( compound ) {
                        sqlWhere.append('(');
                    }
                    appendWhere(item);

                    if ( compound ) {
                        sqlWhere.append(')');
                    }
                }
            }
        }

        private static boolean surrounded(StringBuilder stringBuilder) {
            boolean start = false;
            int count = 0;
            for ( int i = 0; i < stringBuilder.length(); i++ ) {
                char c = stringBuilder.charAt(i);
                boolean whitespace = Character.isWhitespace(c);
                if ( start && count == 0 && !whitespace ) {
                    return false;
                }
                if ( !start && whitespace ) {
                    continue;
                }
                if ( c == '(' ) {
                    count++;
                } else if ( c == ')' ) {
                    count--;
                }

                start = true;

            }
            return count == 0;
        }


        public class PrecompiledSql<T> {

            private final StringBuilder stringBuilder = new StringBuilder();
            private final List<Object> args = new ArrayList<>();
            private final EntityInformation<T, ?> entityInfo;

            public PrecompiledSql(EntityInformation<T, ?> entityInfo) {
                this.entityInfo = entityInfo;
            }

            public void addArgs(Object... args) {
                this.args.addAll(Arrays.asList(args));
            }

            public List<Object> getArgs() {
                return args;
            }

            public StringBuilder getStringBuilder() {
                return stringBuilder;
            }

            public String getSql() {
                return stringBuilder.toString();
            }
        }
    }
}
