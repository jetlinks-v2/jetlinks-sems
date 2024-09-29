package org.jetlinks.pro.sems.utils;

import reactor.core.publisher.Mono;

import java.util.List;

public class TimeSegmentUtil implements Comparable{
    private Long start;

    private Long end;

    private Integer overlapCounter = 0;

    public TimeSegmentUtil(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Integer getOverlapCounter() {
        return overlapCounter;
    }

    @Override
    public int compareTo(Object o) {
        TimeSegmentUtil other = (TimeSegmentUtil) o;
        if (this.end < other.start) {
            return -1;
        } else if (this.start > other.end) {
            return 1;
        }
        overlapCounter++;
        return 0;
    }

    public Mono<Long> judee(List<TimeSegmentUtil> list){

        return Mono.just(list.stream()
            .sorted(TimeSegmentUtil::compareTo)
            .mapToLong(TimeSegmentUtil::getOverlapCounter)
            .sum());

    }
}
