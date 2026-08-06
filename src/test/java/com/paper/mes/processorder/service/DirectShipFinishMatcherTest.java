package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirectShipFinishMatcherTest {

    @Test
    void assign_uuidRelation_matchesWithoutRollNumber() {
        OriginalRoll source = source("roll-1", null);
        FinishRoll finish = finish("finish-1", "A000001", 2);

        Map<String, List<FinishRoll>> result = DirectShipFinishMatcher.assign(
                List.of(source), List.of(finish), List.of(relation(source, finish)));

        assertThat(result).containsEntry("roll-1", List.of(finish));
    }

    @Test
    void assign_multipleBlankSourcesWithLegacyFinish_rejectsAmbiguousPairing() {
        List<OriginalRoll> sources = List.of(source("roll-1", null), source("roll-2", null));
        FinishRoll legacy = finish("finish-1", "A000001", 2);

        assertThatThrownBy(() -> DirectShipFinishMatcher.assign(sources, List.of(legacy), List.of()))
                .hasMessageContaining("无法唯一匹配");
    }

    @Test
    void assign_sameRollNumberSourcesWithLegacyFinish_rejectsAmbiguousPairing() {
        List<OriginalRoll> sources = List.of(source("roll-1", "M001"), source("roll-2", "M001"));
        FinishRoll legacy = finish("finish-1", "M001", 2);

        assertThatThrownBy(() -> DirectShipFinishMatcher.assign(sources, List.of(legacy), List.of()))
                .hasMessageContaining("无法唯一匹配");
    }

    @Test
    void assign_singleUnidentifiedLegacyFinish_rejectsOrderBasedPairing() {
        OriginalRoll source = source("roll-1", "M001");
        FinishRoll legacy = finish("finish-1", "A000001", 2);

        assertThatThrownBy(() -> DirectShipFinishMatcher.assign(
                List.of(source), List.of(legacy), List.of()))
                .hasMessageContaining("不能按顺序匹配");
    }

    @Test
    void assign_multipleLegacyMatchesForSameSource_rejectsAmbiguousIdentifiers() {
        OriginalRoll source = source("roll-1", "M001");
        FinishRoll first = finish("finish-1", "M001", 2);
        FinishRoll second = finish("finish-2", "M001", 2);

        assertThatThrownBy(() -> DirectShipFinishMatcher.assign(
                List.of(source), List.of(first, second), List.of()))
                .hasMessageContaining("多个可能来源");
    }

    @Test
    void assign_moreActiveFinishesThanPieces_rejectsCorruptRelations() {
        OriginalRoll source = source("roll-1", "M001");
        FinishRoll first = finish("finish-1", "A000001", 2);
        FinishRoll second = finish("finish-2", "A000002", 2);

        assertThatThrownBy(() -> DirectShipFinishMatcher.assign(
                List.of(source), List.of(first, second),
                List.of(relation(source, first), relation(source, second))))
                .hasMessageContaining("超过母卷件数");
    }

    @Test
    void assign_twoPiecesWithTwoUuidRelations_returnsBothFinishes() {
        OriginalRoll source = source("roll-1", "M001");
        source.setPieceNum(2);
        FinishRoll first = finish("finish-1", "A000001", 2);
        FinishRoll second = finish("finish-2", "A000002", 2);

        Map<String, List<FinishRoll>> result = DirectShipFinishMatcher.assign(
                List.of(source), List.of(first, second),
                List.of(relation(source, first), relation(source, second)));

        assertThat(result.get("roll-1")).containsExactly(first, second);
    }

    @Test
    void assign_voidedOldFinishAndActiveNewFinish_prefersActiveUuidRelation() {
        OriginalRoll source = source("roll-1", "M001");
        FinishRoll old = finish("finish-old", "A000001", 3);
        FinishRoll current = finish("finish-new", "A000002", 2);

        Map<String, List<FinishRoll>> result = DirectShipFinishMatcher.assign(
                List.of(source), List.of(old, current),
                List.of(relation(source, old), relation(source, current)));

        assertThat(result).containsEntry("roll-1", List.of(current));
    }

    private OriginalRoll source(String uuid, String rollNo) {
        OriginalRoll source = new OriginalRoll();
        source.setUuid(uuid);
        source.setRollNo(rollNo);
        source.setPieceNum(1);
        return source;
    }

    private FinishRoll finish(String uuid, String rollNo, int status) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo(rollNo);
        finish.setRollNoStatus(status);
        return finish;
    }

    private FinishOriginalRel relation(OriginalRoll source, FinishRoll finish) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOriginalUuid(source.getUuid());
        relation.setFinishUuid(finish.getUuid());
        return relation;
    }
}
