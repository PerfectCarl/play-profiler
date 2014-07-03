package models;

public class CallStat {

    private String tag;
    private Integer calls = 0;
    private Long totalTime = 0L;

    public CallStat(String tag) {
        this.tag = tag;
    }

    @SuppressWarnings("unused")
    public String getTag() {
        return tag;
    }

    public Integer getCalls() {
        return calls;
    }

    public void setCalls(Integer calls) {
        this.calls = calls;
    }

    public Long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Long totalTime) {
        this.totalTime = totalTime;
    }

}
