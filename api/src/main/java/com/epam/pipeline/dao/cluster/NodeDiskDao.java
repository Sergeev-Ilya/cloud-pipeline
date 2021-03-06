package com.epam.pipeline.dao.cluster;

import com.epam.pipeline.entity.cluster.NodeDisk;
import com.epam.pipeline.entity.cluster.DiskRegistrationRequest;
import com.epam.pipeline.entity.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NodeDiskDao extends NamedParameterJdbcDaoSupport {
    
    private final String insertNodeDiskQuery;
    private final String loadNodeDisksByNodeIdQuery;

    @Transactional
    public List<NodeDisk> insert(final String nodeId, final List<DiskRegistrationRequest> requests) {
        return insert(nodeId, DateUtils.nowUTC(), requests);
    }

    @Transactional
    public List<NodeDisk> insert(final String nodeId, final LocalDateTime creationDate, 
                                 final List<DiskRegistrationRequest> requests) {
        final List<NodeDisk> disks = toDisks(nodeId, creationDate, requests);
        for (NodeDisk disk : disks) {
            getNamedParameterJdbcTemplate().update(insertNodeDiskQuery, getParameters(disk));
        }
        return disks;
    }

    private List<NodeDisk> toDisks(final String nodeId, final LocalDateTime creationDate,
                                   final List<DiskRegistrationRequest> requests) {
        return requests.stream()
                .map(disk -> new NodeDisk(disk.getSize(), nodeId, creationDate))
                .collect(Collectors.toList());
    }

    public List<NodeDisk> loadByNodeId(final String nodeId) {
        return getJdbcTemplate().query(loadNodeDisksByNodeIdQuery, getRowMapper(), nodeId);
    }

    private MapSqlParameterSource getParameters(final NodeDisk disk) {
        return NodeDiskDao.Parameters.getParameters(disk);
    }
    
    private RowMapper<NodeDisk> getRowMapper() {
        return NodeDiskDao.Parameters.getRowMapper();
    }

    enum Parameters {
        SIZE,
        NODE_ID,
        CREATED_DATE;

        static MapSqlParameterSource getParameters(final NodeDisk disk) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue(SIZE.name(), disk.getSize());
            params.addValue(NODE_ID.name(), disk.getNodeId());
            params.addValue(CREATED_DATE.name(), Timestamp.valueOf(disk.getCreatedDate()));
            return params;
        }

        static RowMapper<NodeDisk> getRowMapper() {
            return (rs, rowNum) -> {
                final Long size = rs.getLong(SIZE.name());
                final String nodeId = rs.getString(NODE_ID.name());
                final LocalDateTime createdDate = rs.getTimestamp(CREATED_DATE.name()).toLocalDateTime();
                return new NodeDisk(size, nodeId, createdDate);
            };
        }
    }
}
