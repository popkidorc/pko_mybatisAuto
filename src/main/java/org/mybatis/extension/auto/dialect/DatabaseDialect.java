package org.mybatis.extension.auto.dialect;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mybatis.extension.auto.annotation.Column;
import org.mybatis.extension.auto.annotation.ForeignKey;
import org.mybatis.extension.auto.annotation.Id;
import org.mybatis.extension.auto.annotation.Table;
import org.mybatis.extension.auto.driver.AutoDataSourceParam;
import org.mybatis.extension.auto.sql.IAlterColumnSql;
import org.mybatis.extension.auto.sql.IAlterForeignKeySql;
import org.mybatis.extension.auto.sql.IAlterPrimaryKeySql;
import org.mybatis.extension.auto.sql.IAutoDataSqlFactory;
import org.mybatis.extension.auto.sql.IConstraintSql;
import org.mybatis.extension.auto.sql.ICreateTableSql;
import org.mybatis.extension.auto.sql.IDropAllConstraintSql;
import org.mybatis.extension.auto.sql.IDropTableSql;
import org.mybatis.extension.auto.sql.entity.ColumnEntity;
import org.mybatis.extension.auto.sql.entity.ForeignKeyEntity;
import org.mybatis.extension.auto.sql.entity.PrimaryKeyEntity;
import org.mybatis.extension.auto.sql.entity.TableEntity;
import org.mybatis.extension.auto.type.AutoType;
import org.mybatis.extension.auto.type.ColumnType;
import org.mybatis.extension.auto.type.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * 
 * Automatically create table dialect interface
 * 
 * @author popkidorc
 * @since 2015年3月28日
 * 
 */
public abstract class DatabaseDialect implements IDatabaseDialect {

	private Logger logger = LoggerFactory.getLogger(getClass());

	protected IAutoDataSqlFactory autoDataSqlFactory;

	protected AutoDataSourceParam autoDataSourceParam;

	protected List<TableEntity> tableEntities;

	protected List<String> sqls;

	/**
	 * 
	 * Constructor
	 * 
	 * @param autoDataSourceParam
	 *            autoDataSource constructor parameters
	 */
	public DatabaseDialect(AutoDataSourceParam autoDataSourceParam) {
		this.autoDataSourceParam = autoDataSourceParam;
	}

	@Override
	public void invoke() throws SQLException {
		this.autoDataSourceParam.getConnection().setAutoCommit(false);
		if (this.autoDataSourceParam.getAuto().equalsIgnoreCase(
				AutoType.CREATE.toString()))
			this.create();
		if (this.autoDataSourceParam.getAuto().equalsIgnoreCase(
				AutoType.UPDATE.toString()))
			this.update();
	}

	/**
	 * 
	 * Create the table using the clazzes , will delete existing data and tables
	 * 
	 * @throws SQLException
	 */
	protected void create() throws SQLException {
		// initialize variable
		this.initializeVariable();
		// disable constraints
		IConstraintSql disableConstraintSql = this.autoDataSqlFactory
				.getConstraintSql();
		disableConstraintSql.init(this.autoDataSourceParam, false);
		this.sqls.addAll(disableConstraintSql.getSqls());

		List<String> dropTableSqls = new ArrayList<String>();
		List<String> createTableSqls = new ArrayList<String>();
		List<String> primaryKeySqls = new ArrayList<String>();
		List<String> foreignKeySqls = new ArrayList<String>();
		for (TableEntity tableEntity : tableEntities) {
			// drop all table
			IDropTableSql dropTableSql = this.autoDataSqlFactory
					.getDropTableSql();
			dropTableSql.init(autoDataSourceParam, tableEntity);
			dropTableSqls.addAll(dropTableSql.getSqls());

			// create table
			ICreateTableSql createTableSql = this.autoDataSqlFactory
					.getCreateTableSql();
			createTableSql.init(autoDataSourceParam, tableEntity);
			createTableSqls.addAll(createTableSql.getSqls());

			// alter primary key & alter primary key type
			IAlterPrimaryKeySql primaryKeySql = this.autoDataSqlFactory
					.getAlterPrimaryKeySql();
			primaryKeySql.init(autoDataSourceParam, tableEntity);
			primaryKeySqls.addAll(primaryKeySql.getSqls());

			// alter foreign keys
			IAlterForeignKeySql foreignKeySql = this.autoDataSqlFactory
					.getAlterForeignKeySql();
			foreignKeySql.init(autoDataSourceParam, tableEntity);
			foreignKeySqls.addAll(foreignKeySql.getSqls());
		}
		this.sqls.addAll(dropTableSqls);
		this.sqls.addAll(createTableSqls);
		this.sqls.addAll(primaryKeySqls);
		this.sqls.addAll(foreignKeySqls);
		// enable constraints
		IConstraintSql enableConstraintSql = this.autoDataSqlFactory
				.getConstraintSql();
		enableConstraintSql.init(this.autoDataSourceParam, true);
		this.sqls.addAll(enableConstraintSql.getSqls());
		this.executeSqls();
	};

	/**
	 * 
	 * Update the table using the clazzes, add or update a column does not
	 * delete the existing data and tables, delete a column will delete the data
	 * in the column
	 * 
	 * @throws SQLException
	 */
	protected void update() throws SQLException {
		// initialize variable
		this.initializeVariable();
		// disable constraints
		IConstraintSql disableConstraintSql = this.autoDataSqlFactory
				.getConstraintSql();
		disableConstraintSql.init(this.autoDataSourceParam, false);
		this.sqls.addAll(disableConstraintSql.getSqls());

		List<String> createTableSqls = new ArrayList<String>();
		List<String> dropAllConstraintSqls = new ArrayList<String>();
		List<String> alterColumnSqls = new ArrayList<String>();
		List<String> primaryKeySqls = new ArrayList<String>();
		List<String> foreignKeySqls = new ArrayList<String>();
		// create table
		for (TableEntity tableEntity : tableEntities) {
			// create table
			ICreateTableSql createTableSql = this.autoDataSqlFactory
					.getCreateTableSql();
			createTableSql.init(autoDataSourceParam, tableEntity);
			createTableSqls.addAll(createTableSql.getSqls());

			// Check column exists & alter column | modify column
			IAlterColumnSql alterColumnSql = this.autoDataSqlFactory
					.getAlterColumnSql();
			alterColumnSql.init(autoDataSourceParam, tableEntity);
			alterColumnSqls.addAll(alterColumnSql.getSqls());

			// query primary keys and foreign keys & drop all foreign keys &
			// drop indexes for foreign keys & drop all primary key
			IDropAllConstraintSql dropAllConstraintSql = this.autoDataSqlFactory
					.getDropAllConstraintSql();
			dropAllConstraintSql.init(autoDataSourceParam, tableEntity);
			dropAllConstraintSqls.addAll(dropAllConstraintSql.getSqls());
			// alter primary key & alter primary key type
			IAlterPrimaryKeySql primaryKeySql = this.autoDataSqlFactory
					.getAlterPrimaryKeySql();
			primaryKeySql.init(autoDataSourceParam, tableEntity);
			primaryKeySqls.addAll(primaryKeySql.getSqls());

			// alter foreign keys
			IAlterForeignKeySql foreignKeySql = this.autoDataSqlFactory
					.getAlterForeignKeySql();
			foreignKeySql.init(autoDataSourceParam, tableEntity);
			foreignKeySqls.addAll(foreignKeySql.getSqls());
		}
		this.sqls.addAll(createTableSqls);
		this.sqls.addAll(dropAllConstraintSqls);
		this.sqls.addAll(alterColumnSqls);
		this.sqls.addAll(primaryKeySqls);
		this.sqls.addAll(foreignKeySqls);
		// enable constraints
		IConstraintSql enableConstraintSql = this.autoDataSqlFactory
				.getConstraintSql();
		enableConstraintSql.init(this.autoDataSourceParam, true);
		this.sqls.addAll(enableConstraintSql.getSqls());
		this.executeSqls();
	};

	/**
	 * initialize variable & initialize variable
	 */
	private void initializeVariable() {
		// initialize variable
		this.sqls = new ArrayList<String>();
		this.tableEntities = new ArrayList<TableEntity>();
		// parse classes
		for (Class<?> clazz : this.autoDataSourceParam.getClazzes()) {
			if (!clazz.isAnnotationPresent(Table.class)) {
				continue;
			}
			TableEntity tableEntity = new TableEntity();
			Table tableAnnotion = (Table) clazz.getAnnotation(Table.class);
			tableEntity
					.setTableName(tableAnnotion.tableName().equals("") ? ClassUtils
							.getShortName(clazz).toUpperCase() : tableAnnotion
							.tableName());
			tableEntity.setTableComment(tableAnnotion.comment());
			tableEntity.setEngine(tableAnnotion.engine());
			tableEntity.setDefaultCharset(tableAnnotion.defaultCharset());

			List<ColumnEntity> columnEntities = new ArrayList<ColumnEntity>();
			PrimaryKeyEntity primaryKeyEntity = new PrimaryKeyEntity();
			List<ForeignKeyEntity> foreignKeyEntites = new ArrayList<ForeignKeyEntity>();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAnnotationPresent(Column.class)) {
					continue;
				}
				ColumnEntity columnEntity = new ColumnEntity();
				Column fieldAnnotion = (Column) field
						.getAnnotation(Column.class);
				columnEntity.setColumnName(fieldAnnotion.columnName()
						.equals("") ? field.getName().toUpperCase()
						: fieldAnnotion.columnName());
				columnEntity.setColumnComment(fieldAnnotion.comment());
				this.autoDataSqlFactory.getColumnTypeMap();
				columnEntity.setColumnType(fieldAnnotion.type().equals(
						ColumnType.AUTO) ? this.autoDataSqlFactory
						.getColumnTypeMap().getColumnType(field.getType())
						.toString() : fieldAnnotion.type().toString());
				columnEntity.setColumnLength(columnEntity.getColumnType()
						.equals(ColumnType.INT.toString())
						&& fieldAnnotion.length() > 11 ? 11 : fieldAnnotion
						.length());
				columnEntity.setColumnNullable(fieldAnnotion.nullable() ? ""
						: "NOT NULL");
				boolean isPrimaryKey = field.isAnnotationPresent(Id.class);
				columnEntity.setPrimaryKey(isPrimaryKey);
				if (isPrimaryKey) {
					Id idAnnotation = field.getAnnotation(Id.class);
					columnEntity.setPrimaryKeyType(idAnnotation.idType()
							.equals(IdType.SIMPLE) ? "" : idAnnotation.idType()
							.toString());
					primaryKeyEntity.setTableName(tableEntity.getTableName());
					primaryKeyEntity.setPrimaryKeyName(idAnnotation
							.primaryKeyName().equals("") ? "PK_"
							+ tableEntity.getTableName() + "_"
							+ columnEntity.getColumnName() : idAnnotation
							.primaryKeyName());
					primaryKeyEntity.addPrimaryKeyColumn(columnEntity);
				}
				ForeignKeyEntity foreignKeyEntity = new ForeignKeyEntity();
				for (ForeignKey foreignKey : fieldAnnotion.fKey()) {
					foreignKeyEntity.setForeignKeyName(foreignKey
							.foreignKeyName().equals("") ? "FK_"
							+ tableEntity.getTableName() + "_"
							+ columnEntity.getColumnName() : foreignKey
							.foreignKeyName());
					foreignKeyEntity
							.setColumnName(columnEntity.getColumnName());
					foreignKeyEntity.setTableName(tableEntity.getTableName());
					foreignKeyEntity.setForeignKeyColumnName(foreignKey
							.columnName());
					foreignKeyEntity.setForeignKeyTableName(foreignKey
							.tableName());
					foreignKeyEntites.add(foreignKeyEntity);
				}
				columnEntities.add(columnEntity);
			}
			tableEntity.setPrimaryKeyEntity(primaryKeyEntity);
			tableEntity.setForeignKeyEntites(foreignKeyEntites);
			tableEntity.setColumnEntities(columnEntities);
			tableEntities.add(tableEntity);
		}
	}

	/**
	 * Execute SQL statements
	 */
	private void executeSqls() {
		try {
			this.autoDataSourceParam.getConnection().setAutoCommit(false);
			Statement statement = this.autoDataSourceParam.getConnection()
					.createStatement();
			for (String sql : this.sqls) {
				if (this.autoDataSourceParam.isShowSql()) {
					logger.info("mybatiSql : " + sql);
				}
				statement.addBatch(sql);
			}
			statement.executeBatch();
			this.autoDataSourceParam.getConnection().commit();
		} catch (SQLException e) {
			logger.error("autoDatabase:" + e.getMessage());
			e.printStackTrace();
			try {
				this.autoDataSourceParam.getConnection().rollback();
			} catch (SQLException e1) {
				logger.error("autoDatabase:" + e1.getMessage());
				e.printStackTrace();
			}
		} finally {
			try {
				this.autoDataSourceParam.getConnection().setAutoCommit(true);
				this.autoDataSourceParam.getConnection().close();
			} catch (SQLException e) {
				logger.error("autoDatabase:" + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
