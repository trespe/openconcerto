package org.jdom;

import java.util.Iterator;

import org.jdom.filter.Filter;

// make public
public class PFilterIterator extends FilterIterator {
    public PFilterIterator(Iterator<?> iterator, Filter filter) {
        super(iterator, filter);
    }
}
