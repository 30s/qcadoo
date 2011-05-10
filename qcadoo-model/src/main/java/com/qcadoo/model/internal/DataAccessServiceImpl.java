/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.model.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.qcadoo.model.api.CopyException;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityList;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.ExpressionService;
import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.aop.Monitorable;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.model.api.types.HasManyType;
import com.qcadoo.model.api.types.TreeType;
import com.qcadoo.model.api.validators.ErrorMessage;
import com.qcadoo.model.internal.api.DataAccessService;
import com.qcadoo.model.internal.api.EntityService;
import com.qcadoo.model.internal.api.InternalDataDefinition;
import com.qcadoo.model.internal.api.PriorityService;
import com.qcadoo.model.internal.api.ValidationService;
import com.qcadoo.model.internal.search.Order;
import com.qcadoo.model.internal.search.Restriction;
import com.qcadoo.model.internal.search.SearchCriteria;
import com.qcadoo.model.internal.search.SearchResultImpl;
import com.qcadoo.tenant.api.Standalone;

@Service
@Standalone
public class DataAccessServiceImpl implements DataAccessService {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private EntityService entityService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private ExpressionService expressionService;

    private static final Logger LOG = LoggerFactory.getLogger(DataAccessServiceImpl.class);

    @Override
    @Transactional
    public Entity save(final InternalDataDefinition dataDefinition, final Entity genericEntity) {
        Set<Entity> newlySavedEntities = new HashSet<Entity>();
        Entity resultEntity = performSave(dataDefinition, genericEntity, new HashSet<Entity>(), newlySavedEntities);
        try {
            if (TransactionAspectSupport.currentTransactionStatus().isRollbackOnly()) {
                resultEntity.setNotValid();
                for (Entity e : newlySavedEntities) {
                    e.setId(null);
                }
            }
        } catch (NoTransactionException e) {
            // nothing - test purpose only
        }
        return resultEntity;
    }

    @SuppressWarnings("unchecked")
    @Monitorable
    private Entity performSave(final InternalDataDefinition dataDefinition, final Entity genericEntity,
            final Set<Entity> alreadySavedEntities, final Set<Entity> newlySavedEntities) {

        checkNotNull(dataDefinition, "DataDefinition must be given");
        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");
        checkNotNull(genericEntity, "Entity must be given");

        if (alreadySavedEntities.contains(genericEntity)) {
            return genericEntity;
        }
        Entity genericEntityToSave = genericEntity.copy();

        Object existingDatabaseEntity = getExistingDatabaseEntity(dataDefinition, genericEntity);

        Entity existingGenericEntity = null;

        if (existingDatabaseEntity != null) {
            existingGenericEntity = entityService.convertToGenericEntity(dataDefinition, existingDatabaseEntity);
        }

        validationService.validateGenericEntity(dataDefinition, genericEntity, existingGenericEntity);

        if (!genericEntity.isValid()) {
            copyValidationErrors(dataDefinition, genericEntityToSave, genericEntity);
            if (existingGenericEntity != null) {
                copyMissingFields(genericEntityToSave, existingGenericEntity);
            }

            LOG.info(genericEntityToSave + " hasn't been saved, bacause of validation errors");

            if (LOG.isDebugEnabled()) {
                for (ErrorMessage error : genericEntityToSave.getGlobalErrors()) {
                    LOG.debug(" --- " + error.getMessage());
                }
                for (Map.Entry<String, ErrorMessage> error : genericEntityToSave.getErrors().entrySet()) {
                    LOG.debug(" --- " + error.getKey() + ": " + error.getValue().getMessage());
                }
            }

            return genericEntityToSave;
        }

        Object databaseEntity = entityService.convertToDatabaseEntity(dataDefinition, genericEntity, existingDatabaseEntity);

        if (genericEntity.getId() == null) {
            priorityService.prioritizeEntity(dataDefinition, databaseEntity);
        }

        saveDatabaseEntity(dataDefinition, databaseEntity);

        Entity savedEntity = entityService.convertToGenericEntity(dataDefinition, databaseEntity);

        LOG.info(savedEntity + " has been saved");

        for (Entry<String, FieldDefinition> fieldEntry : dataDefinition.getFields().entrySet()) {
            if (fieldEntry.getValue().getType() instanceof HasManyType) {
                List<Entity> entities = (List<Entity>) genericEntity.getField(fieldEntry.getKey());

                HasManyType hasManyType = (HasManyType) fieldEntry.getValue().getType();

                if (entities == null || entities instanceof EntityListImpl) {
                    savedEntity.setField(fieldEntry.getKey(), entities);
                    continue;
                }

                List<Entity> savedEntities = saveHasManyEntities(alreadySavedEntities, newlySavedEntities,
                        hasManyType.getJoinFieldName(), savedEntity.getId(), entities,
                        (InternalDataDefinition) hasManyType.getDataDefinition());

                EntityList dbEntities = savedEntity.getHasManyField(fieldEntry.getKey());

                removeOrphans(savedEntities, (InternalDataDefinition) hasManyType.getDataDefinition(), dbEntities);

                savedEntity.setField(fieldEntry.getKey(), savedEntities);
            } else if (fieldEntry.getValue().getType() instanceof TreeType) {
                List<Entity> entities = (List<Entity>) genericEntity.getField(fieldEntry.getKey());

                if (entities == null || entities instanceof EntityTreeImpl) {
                    savedEntity.setField(fieldEntry.getKey(), entities);
                    continue;
                }

                TreeType treeType = (TreeType) fieldEntry.getValue().getType();

                List<Entity> savedEntities = saveTreeEntities(alreadySavedEntities, newlySavedEntities,
                        treeType.getJoinFieldName(), savedEntity.getId(), entities,
                        (InternalDataDefinition) treeType.getDataDefinition(), null);

                savedEntity.setField(fieldEntry.getKey(), savedEntities);
            }
        }

        alreadySavedEntities.add(savedEntity);

        if (genericEntity.getId() == null && savedEntity.getId() != null) {
            newlySavedEntities.add(savedEntity);
        }

        return savedEntity;
    }

    private List<Entity> saveHasManyEntities(final Set<Entity> alreadySavedEntities, final Set<Entity> newlySavedEntities,
            final String joinFieldName, final Long id, final List<Entity> entities, final InternalDataDefinition dataDefinition) {
        List<Entity> savedEntities = new ArrayList<Entity>();

        for (Entity innerEntity : entities) {
            innerEntity.setField(joinFieldName, id);
            Entity savedInnerEntity = performSave(dataDefinition, innerEntity, alreadySavedEntities, newlySavedEntities);
            savedEntities.add(savedInnerEntity);
            if (!savedInnerEntity.isValid()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        }

        return savedEntities;
    }

    @SuppressWarnings("unchecked")
    private List<Entity> saveTreeEntities(final Set<Entity> alreadySavedEntities, final Set<Entity> newlySavedEntities,
            final String joinFieldName, final Long id, final List<Entity> entities, final InternalDataDefinition dataDefinition,
            final Long parentId) {
        List<Entity> savedEntities = new ArrayList<Entity>();
        int i = 0;

        for (Entity innerEntity : entities) {
            innerEntity.setField(joinFieldName, id);
            innerEntity.setField("parent", parentId);
            innerEntity.setField("priority", ++i);
            List<Entity> children = (List<Entity>) innerEntity.getField("children");
            innerEntity.setField("children", null);
            Entity savedInnerEntity = performSave(dataDefinition, innerEntity, alreadySavedEntities, newlySavedEntities);
            savedEntities.add(savedInnerEntity);
            if (children != null) {
                children = saveTreeEntities(alreadySavedEntities, newlySavedEntities, joinFieldName, id, children,
                        dataDefinition, savedInnerEntity.getId());
                savedInnerEntity.setField("children", children);
            }
            if (!savedInnerEntity.isValid()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        }

        return savedEntities;
    }

    private void removeOrphans(final List<Entity> savedEntities, final InternalDataDefinition dataDefinition,
            final List<Entity> dbEntities) {
        for (Entity dbEntity : dbEntities) {
            boolean exists = false;
            for (Entity exisingEntity : savedEntities) {
                if (dbEntity.getId().equals(exisingEntity.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                delete(dataDefinition, dbEntity.getId());
            }
        }
    }

    @Override
    @Transactional
    @Monitorable
    public List<Entity> copy(final InternalDataDefinition dataDefinition, final Long... entityIds) {
        List<Entity> copiedEntities = new ArrayList<Entity>();
        for (Long entityId : entityIds) {
            Entity sourceEntity = get(dataDefinition, entityId);
            Entity targetEntity = copy(dataDefinition, sourceEntity);

            if (targetEntity == null) {
                throw new IllegalStateException("Cannot copy " + sourceEntity);
            }

            LOG.info(sourceEntity + " has been copied to " + targetEntity);

            targetEntity = save(dataDefinition, targetEntity);

            if (!targetEntity.isValid()) {
                throw new CopyException(targetEntity);
            }

            copiedEntities.add(targetEntity);
        }

        return copiedEntities;
    }

    public Entity copy(final InternalDataDefinition dataDefinition, final Entity sourceEntity) {
        Entity targetEntity = dataDefinition.create();

        for (String fieldName : dataDefinition.getFields().keySet()) {
            targetEntity.setField(fieldName, getCopyValueOfSimpleField(sourceEntity, dataDefinition, fieldName));
        }

        if (!dataDefinition.callCopyHook(targetEntity)) {
            return null;
        }

        for (String fieldName : dataDefinition.getFields().keySet()) {
            copyHasManyField(sourceEntity, targetEntity, dataDefinition, fieldName);
        }

        for (String fieldName : dataDefinition.getFields().keySet()) {
            copyTreeField(sourceEntity, targetEntity, dataDefinition, fieldName);
        }

        return targetEntity;
    }

    private void copyTreeField(final Entity sourceEntity, final Entity targetEntity, final DataDefinition dataDefinition,
            final String fieldName) {
        FieldDefinition fieldDefinition = dataDefinition.getField(fieldName);

        if (!(fieldDefinition.getType() instanceof TreeType) || !((TreeType) fieldDefinition.getType()).isCopyable()) {
            return;
        }

        TreeType treeType = ((TreeType) fieldDefinition.getType());

        List<Entity> entities = new ArrayList<Entity>();

        Entity root = sourceEntity.getTreeField(fieldName).getRoot();

        if (root != null) {
            root.setField(treeType.getJoinFieldName(), null);
            root = copy((InternalDataDefinition) treeType.getDataDefinition(), root);

            if (root != null) {
                entities.add(root);
            }
        }

        targetEntity.setField(fieldName, entities);
    }

    private void copyHasManyField(final Entity sourceEntity, final Entity targetEntity, final DataDefinition dataDefinition,
            final String fieldName) {
        FieldDefinition fieldDefinition = dataDefinition.getField(fieldName);

        if (!(fieldDefinition.getType() instanceof HasManyType) || !((HasManyType) fieldDefinition.getType()).isCopyable()) {
            return;
        }

        HasManyType hasManyType = ((HasManyType) fieldDefinition.getType());

        List<Entity> entities = new ArrayList<Entity>();

        for (Entity childEntity : sourceEntity.getHasManyField(fieldName)) {
            childEntity.setField(hasManyType.getJoinFieldName(), null);

            Entity savedChildEntity = copy((InternalDataDefinition) hasManyType.getDataDefinition(), childEntity);

            if (savedChildEntity != null) {
                entities.add(savedChildEntity);
            }
        }

        targetEntity.setField(fieldName, entities);
    }

    private Object getCopyValueOfSimpleField(final Entity sourceEntity, final DataDefinition dataDefinition,
            final String fieldName) {
        FieldDefinition fieldDefinition = dataDefinition.getField(fieldName);
        if (fieldDefinition.isUnique()) {
            if (fieldDefinition.getType().getType().equals(String.class)) {
                return getCopyValueOfUniqueField(dataDefinition, fieldDefinition, sourceEntity.getStringField(fieldName));
            } else {
                sourceEntity.addError(fieldDefinition, "core.validate.field.error.invalidUniqueType");
                throw new CopyException(sourceEntity);
            }
        } else if (fieldDefinition.getType() instanceof HasManyType) {
            return null;
        } else if (fieldDefinition.getType() instanceof TreeType) {
            return null;
        } else {
            return sourceEntity.getField(fieldName);
        }
    }

    private String getCopyValueOfUniqueField(final DataDefinition dataDefinition, final FieldDefinition fieldDefinition,
            final String value) {
        Matcher matcher = Pattern.compile("(.+)\\((\\d+)\\)").matcher(value);

        String oldValue = value;
        int index = 1;

        if (matcher.matches()) {
            oldValue = matcher.group(1);
            index = Integer.valueOf(matcher.group(2)) + 1;
        }

        while (true) {
            String newValue = oldValue + "(" + (index++) + ")";

            int matches = dataDefinition.find().setMaxResults(1).isEq(fieldDefinition.getName(), newValue).list()
                    .getTotalNumberOfEntities();

            if (matches == 0) {
                return newValue;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Monitorable
    public Entity get(final InternalDataDefinition dataDefinition, final Long entityId) {
        checkNotNull(dataDefinition, "DataDefinition must be given");
        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");
        checkNotNull(entityId, "EntityId must be given");

        Object databaseEntity = getDatabaseEntity(dataDefinition, entityId);

        if (databaseEntity == null) {
            LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                    + "] hasn't been retrieved, because it doesn't exist");
            return null;
        }

        Entity entity = entityService.convertToGenericEntity(dataDefinition, databaseEntity);

        LOG.info(entity + " has been retrieved");

        return entity;
    }

    @Override
    @Transactional
    // @Check(constraints = "flag = true")
    @Monitorable
    public void delete(final InternalDataDefinition dataDefinition, final Long... entityIds) {
        checkNotNull(dataDefinition, "DataDefinition must be given");
        checkState(dataDefinition.isDeletable(), "Entity must be deletable");
        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");
        checkState(entityIds.length > 0, "EntityIds must be given");

        for (Long entityId : entityIds) {
            deleteEntity(dataDefinition, entityId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Monitorable
    public SearchResult find(final SearchCriteria searchCriteria) {
        checkArgument(searchCriteria != null, "SearchCriteria must be given");

        InternalDataDefinition dataDefinition = (InternalDataDefinition) searchCriteria.getDataDefinition();

        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");

        int totalNumberOfEntities = getTotalNumberOfEntities(getCriteria(searchCriteria));

        if (totalNumberOfEntities == 0 || searchCriteria.getRestrictions().contains(null)) {
            LOG.info("There is no entity matching criteria " + searchCriteria);
            return getResultSet(searchCriteria, dataDefinition, totalNumberOfEntities, Collections.emptyList());
        }

        Criteria criteria = getCriteria(searchCriteria).setFirstResult(searchCriteria.getFirstResult()).setMaxResults(
                searchCriteria.getMaxResults());

        addOrderToCriteria(searchCriteria.getOrder(), criteria);

        List<?> results = criteria.list();

        LOG.info("There are " + totalNumberOfEntities + " entities matching criteria " + searchCriteria);

        return getResultSet(searchCriteria, dataDefinition, totalNumberOfEntities, results);
    }

    @Override
    @Transactional
    @Monitorable
    public void moveTo(final InternalDataDefinition dataDefinition, final Long entityId, final int position) {
        checkNotNull(dataDefinition, "DataDefinition must be given");
        checkState(dataDefinition.isPrioritizable(), "Entity must be prioritizable");
        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");
        checkNotNull(entityId, "EntityId must be given");
        checkState(position > 0, "Position must be greaten than 0");

        Object databaseEntity = getDatabaseEntity(dataDefinition, entityId);

        if (databaseEntity == null) {
            LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                    + "] hasn't been prioritized, because it doesn't exist");
            return;
        }

        priorityService.move(dataDefinition, databaseEntity, position, 0);

        LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                + "] has been prioritized");
    }

    @Override
    @Transactional
    @Monitorable
    public void move(final InternalDataDefinition dataDefinition, final Long entityId, final int offset) {
        checkNotNull(dataDefinition, "DataDefinition must be given");
        checkState(dataDefinition.isPrioritizable(), "Entity must be prioritizable");
        checkState(dataDefinition.isEnabled(), "DataDefinition belongs to disabled plugin");
        checkNotNull(entityId, "EntityId must be given");
        checkState(offset != 0, "Offset must be different than 0");

        Object databaseEntity = getDatabaseEntity(dataDefinition, entityId);

        if (databaseEntity == null) {
            LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                    + "] hasn't been prioritized, because it doesn't exist");
            return;
        }

        priorityService.move(dataDefinition, databaseEntity, 0, offset);

        LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                + "] has been prioritized");
    }

    private Object getExistingDatabaseEntity(final InternalDataDefinition dataDefinition, final Entity entity) {
        Object existingDatabaseEntity = null;

        if (entity.getId() != null) {
            existingDatabaseEntity = getDatabaseEntity(dataDefinition, entity.getId());
            checkState(existingDatabaseEntity != null, "Entity[%s][id=%s] cannot be found", dataDefinition.getPluginIdentifier()
                    + "." + dataDefinition.getName(), entity.getId());
        }

        return existingDatabaseEntity;
    }

    private void deleteEntity(final InternalDataDefinition dataDefinition, final Long entityId) {

        Object databaseEntity = getDatabaseEntity(dataDefinition, entityId);

        checkNotNull(databaseEntity, "Entity[%s][id=%s] cannot be found", dataDefinition.getPluginIdentifier() + "."
                + dataDefinition.getName(), entityId);

        Entity entity = get(dataDefinition, entityId);

        priorityService.deprioritizeEntity(dataDefinition, databaseEntity);
        Map<String, FieldDefinition> fields = dataDefinition.getFields();

        for (FieldDefinition fieldDefinition : fields.values()) {
            if (fieldDefinition.getType() instanceof HasManyType) {
                HasManyType hasManyFieldType = (HasManyType) fieldDefinition.getType();
                EntityList children = entity.getHasManyField(fieldDefinition.getName());
                InternalDataDefinition childDataDefinition = (InternalDataDefinition) hasManyFieldType.getDataDefinition();
                if (HasManyType.Cascade.NULLIFY.equals(hasManyFieldType.getCascade())) {
                    for (Entity child : children) {
                        child.setField(hasManyFieldType.getJoinFieldName(), null);
                        child = save(childDataDefinition, child);
                        if (!child.isValid()) {
                            throw new IllegalStateException(String.format("Entity [ENTITY.%s] is in use",
                                    expressionService.getValue(entity, dataDefinition.getIdentifierExpression(), Locale.ENGLISH)));
                        }
                    }
                }
            }

            if (fieldDefinition.getType() instanceof TreeType) {
                TreeType treeFieldType = (TreeType) fieldDefinition.getType();
                EntityTree children = entity.getTreeField(fieldDefinition.getName());
                InternalDataDefinition childDataDefinition = (InternalDataDefinition) treeFieldType.getDataDefinition();
                if (TreeType.Cascade.NULLIFY.equals(treeFieldType.getCascade())) {
                    for (Entity child : children) {
                        child.setField(treeFieldType.getJoinFieldName(), null);
                        child = save(childDataDefinition, child);
                        if (!child.isValid()) {
                            throw new IllegalStateException(String.format("Entity [ENTITY.%s] is in use",
                                    expressionService.getValue(entity, dataDefinition.getIdentifierExpression(), Locale.ENGLISH)));
                        }
                    }
                }
            }
        }
        try {
            getCurrentSession().delete(databaseEntity);
            getCurrentSession().flush();
        } catch (ConstraintViolationException e) {
            throw new IllegalStateException(String.format("Entity [ENTITY.%s] is in use",
                    expressionService.getValue(entity, dataDefinition.getIdentifierExpression(), Locale.ENGLISH)), e);
        }

        LOG.info("Entity[" + dataDefinition.getPluginIdentifier() + "." + dataDefinition.getName() + "][id=" + entityId
                + "] has been deleted");
    }

    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    protected Criteria getCriteria(final SearchCriteria searchCriteria) {
        InternalDataDefinition dataDefinition = (InternalDataDefinition) searchCriteria.getDataDefinition();
        Criteria criteria = getCurrentSession().createCriteria(dataDefinition.getClassForEntity());

        for (Restriction restriction : searchCriteria.getRestrictions()) {
            criteria.add(addRestrictionToCriteria(restriction, criteria));
        }

        return criteria;
    }

    private int getTotalNumberOfEntities(final Criteria criteria) {
        return Integer.valueOf(criteria.setProjection(Projections.rowCount()).uniqueResult().toString());
    }

    private SearchResultImpl getResultSet(final SearchCriteria searchCriteria, final InternalDataDefinition dataDefinition,
            final int totalNumberOfEntities, final List<?> results) {
        List<Entity> genericResults = new ArrayList<Entity>();

        for (Object databaseEntity : results) {
            genericResults.add(entityService.convertToGenericEntity(dataDefinition, databaseEntity));
        }

        SearchResultImpl resultSet = new SearchResultImpl();
        resultSet.setResults(genericResults);
        resultSet.setTotalNumberOfEntities(totalNumberOfEntities);

        return resultSet;
    }

    private Criterion addRestrictionToCriteria(final Restriction restriction, final Criteria criteria) {
        return restriction.addToHibernateCriteria(criteria);
    }

    private Criteria addOrderToCriteria(final Order order, final Criteria criteria) {
        String[] path = order.getFieldName().split("\\.");

        if (path.length > 2) {
            throw new IllegalStateException("Cannot order using multiple assosiations - " + order.getFieldName());
        } else if (path.length == 2 && !criteria.toString().matches(".*Subcriteria\\(" + path[0] + ":" + path[0] + "\\).*")) {
            criteria.createAlias(path[0], path[0]);
        }

        if (order.isAsc()) {
            return criteria.addOrder(org.hibernate.criterion.Order.asc(order.getFieldName()).ignoreCase());
        } else {
            return criteria.addOrder(org.hibernate.criterion.Order.desc(order.getFieldName()).ignoreCase());
        }
    }

    protected Object getDatabaseEntity(final InternalDataDefinition dataDefinition, final Long entityId) {
        return getCurrentSession().get(dataDefinition.getClassForEntity(), entityId);
    }

    protected void saveDatabaseEntity(final InternalDataDefinition dataDefinition, final Object databaseEntity) {
        getCurrentSession().save(databaseEntity);
    }

    private void copyMissingFields(final Entity genericEntityToSave, final Entity existingGenericEntity) {
        for (Map.Entry<String, Object> field : existingGenericEntity.getFields().entrySet()) {
            if (!genericEntityToSave.getFields().containsKey(field.getKey())) {
                genericEntityToSave.setField(field.getKey(), field.getValue());
            }
        }
    }

    private void copyValidationErrors(final DataDefinition dataDefinition, final Entity genericEntityToSave,
            final Entity genericEntity) {
        for (ErrorMessage error : genericEntity.getGlobalErrors()) {
            genericEntityToSave.addGlobalError(error.getMessage(), error.getVars());
        }
        for (Map.Entry<String, ErrorMessage> error : genericEntity.getErrors().entrySet()) {
            genericEntityToSave.addError(dataDefinition.getField(error.getKey()), error.getValue().getMessage(), error.getValue()
                    .getVars());
        }
    }

    protected void setEntityService(final EntityService entityService) {
        this.entityService = entityService;
    }

    protected void setExpressionService(final ExpressionService expressionService) {
        this.expressionService = expressionService;
    }

    protected void setPriorityService(final PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    protected void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected void setValidationService(final ValidationService validationService) {
        this.validationService = validationService;
    }

}
