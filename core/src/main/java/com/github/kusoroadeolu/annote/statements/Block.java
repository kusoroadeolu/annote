package com.github.kusoroadeolu.annote.statements;

import java.util.List;

/**
 * @param parentStatement The If/Loop that owns this block
 */
public record Block(List<Statement> currentBlock, Statement parentStatement) {
    Block(List<Statement> block) {
        this(block, null);
    }

    public void add(Statement stmt) {
        currentBlock.add(stmt);
    }
}