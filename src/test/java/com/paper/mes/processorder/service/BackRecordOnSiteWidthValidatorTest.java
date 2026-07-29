package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackRecordOnSiteWidthValidatorTest {

    @Test
    void validate_outputsWithinPieceCapacity_acceptsSubmission() {
        OriginalRoll source = source(1200, 2);
        FinishRoll first = finish("finish-1", 1200, 2, 2);
        FinishRoll second = finish("finish-2", 1200, 2, 2);

        assertThatCode(() -> BackRecordOnSiteWidthValidator.validate(
                List.of(source), List.of(first, second),
                List.of(relation(source, first), relation(source, second))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_outputsBeyondPieceCapacity_rejectsSubmission() {
        OriginalRoll source = source(1200, 2);
        FinishRoll first = finish("finish-1", 1200, 2, 2);
        FinishRoll second = finish("finish-2", 1201, 2, 2);

        assertThatThrownBy(() -> BackRecordOnSiteWidthValidator.validate(
                List.of(source), List.of(first, second),
                List.of(relation(source, first), relation(source, second))))
                .hasMessageContaining("2400mm");
    }

    @Test
    void validate_voidedOutput_doesNotConsumeCapacity() {
        OriginalRoll source = source(1200, 1);
        FinishRoll active = finish("finish-1", 1200, 2, 2);
        FinishRoll voided = finish("finish-2", 900, 3, 4);

        assertThatCode(() -> BackRecordOnSiteWidthValidator.validate(
                List.of(source), List.of(active, voided),
                List.of(relation(source, active), relation(source, voided))))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_duplicateRelationPair_countsOutputOnce() {
        OriginalRoll source = source(1200, 1);
        FinishRoll finish = finish("finish-1", 1200, 2, 2);
        FinishOriginalRel relation = relation(source, finish);

        assertThatCode(() -> BackRecordOnSiteWidthValidator.validate(
                List.of(source), List.of(finish), List.of(relation, relation)))
                .doesNotThrowAnyException();
    }

    private OriginalRoll source(int width, int pieces) {
        OriginalRoll source = new OriginalRoll();
        source.setUuid("roll-1");
        source.setRollNo("M001");
        source.setProcessMode(2);
        source.setActualWidth(width);
        source.setPieceNum(pieces);
        return source;
    }

    private FinishRoll finish(String uuid, int width, int rollNoStatus, int finishStatus) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishWidth(width);
        finish.setRollNoStatus(rollNoStatus);
        finish.setFinishStatus(finishStatus);
        return finish;
    }

    private FinishOriginalRel relation(OriginalRoll source, FinishRoll finish) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOriginalUuid(source.getUuid());
        relation.setFinishUuid(finish.getUuid());
        return relation;
    }
}
