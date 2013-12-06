INSERT INTO flownode_instance
            (tenantid,
            id,
            flownodeDefinitionId,
            kind,
            rootContainerId,
            parentContainerId,
            name,
            stateId,
            stateName,
            prev_state_id,
            terminal,
            stable,
            actorId,
            assigneeId,
            reachedStateDate,
            lastUpdateDate,
            expectedEndDate,
            claimedDate,
            priority,
            gatewayType,
            hitBys,
            stateCategory,
            logicalGroup1,
            logicalGroup2,
            logicalGroup3,
            logicalGroup4,
            loop_counter,
            executedBy,
            state_executing,
            abortedByBoundary,
            interrupting,
            deleted,
            tokenCount,
            token_ref_id,
            executedByDelegate)
           VALUES
            (
            :tenantid,
            :id,
            :flownodeDefinitionId,
            ':kind',
            :rootContainerId,
            :parentContainerId,
            ':name',
            :stateId,
            ':stateName',
            :stateId,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            :gatewayType,
            :hitBys,
            ':stateCategory',
            :logicalGroup1,
            :logicalGroup2,
            :logicalGroup3,
            :logicalGroup4,
            -1,
            0,
            0,
            0,
            0,
            0,
            0,
            :token_ref_id,
            0
            )