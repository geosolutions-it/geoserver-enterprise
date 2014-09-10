/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * The Class ProcessDescriptor.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
@Entity(name = "ProcessDescriptor")
@Table(name = "processdescriptor")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "processdescriptor")
@XmlRootElement(name = "ProcessDescriptor")
@XmlType(propOrder = { "id", "clusterId", "executionId", "status", "phase", "progress", "result" })
public class ProcessDescriptor implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3654914559439623648L;

    /** The id. */
    @Id
    @GeneratedValue
    @Column
    private Long id;

    /** The cluster id. */
    @Column(nullable = false, updatable = true)
    private String clusterId;

    /** The execution id. */
    @Column(nullable = false, updatable = true)
    private String executionId;

    /** The phase. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = true)
    private ProcessState phase;

    /** The progress. */
    @Column(nullable = false, updatable = true)
    private float progress;

    /** The result. */
    @Lob
    @Column(nullable = true, updatable = true)
    private String result;

    @Column(nullable = true, updatable = true)
    private Date startTime;

    @Column(nullable = true, updatable = true)
    private Date finishTime;

    @Column(nullable = true, updatable = true)
    private Date lastUpdateTime;

    /** The email of the caller. */
    @Column(nullable = true, updatable = true)
    private String email;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private String nameSpace;

    @Lob
    @Column(nullable = true, updatable = true)
    private String output;

    /**
     * Instantiates a new instance.
     */
    public ProcessDescriptor() {

    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id the new id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Sets the cluster id.
     * 
     * @param clusterId the new cluster id
     */
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * Gets the cluster id.
     * 
     * @return the cluster id
     */
    public String getClusterId() {
        return clusterId;
    }

    /**
     * Sets the execution id.
     * 
     * @param executionId the new execution id
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * Gets the execution id.
     * 
     * @return the execution id
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Sets the phase.
     * 
     * @param phase the new phase
     */
    public void setPhase(ProcessState phase) {
        this.phase = phase;
    }

    /**
     * Gets the phase.
     * 
     * @return the phase
     */
    public ProcessState getPhase() {
        return phase;
    }

    /**
     * Sets the progress.
     * 
     * @param progress the new progress
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    /**
     * Gets the progress.
     * 
     * @return the progress
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Sets the result.
     * 
     * @param result the new result
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * Gets the result.
     * 
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * @return the finishTime
     */
    public Date getFinishTime() {
        return finishTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * @param finishTime the finishTime to set
     */
    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * @return the lastUpdateTime
     */
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * @param lastUpdateTime the lastUpdateTime to set
     */
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the nameSpace
     */
    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * @param name the name to set
     */
    public void setName(String processName) {
        this.name = processName;
    }

    /**
     * @param nameSpace the nameSpace to set
     */
    public void setNameSpace(String processNameSpace) {
        this.nameSpace = processNameSpace;
    }

    /**
     * @return the output
     */
    public String getOutput() {
        return output;
    }

    /**
     * @param output the output to set
     */
    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProcessDescriptor [");
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (clusterId != null) {
            builder.append("clusterId=");
            builder.append(clusterId);
            builder.append(", ");
        }
        if (executionId != null) {
            builder.append("executionId=");
            builder.append(executionId);
            builder.append(", ");
        }
        if (phase != null) {
            builder.append("phase=");
            builder.append(phase);
            builder.append(", ");
        }
        builder.append("progress=");
        builder.append(progress);
        builder.append(", ");
        if (result != null) {
            builder.append("result=");
            builder.append(result);
            builder.append(", ");
        }
        if (startTime != null) {
            builder.append("startTime=");
            builder.append(startTime);
            builder.append(", ");
        }
        if (finishTime != null) {
            builder.append("finishTime=");
            builder.append(finishTime);
            builder.append(", ");
        }
        if (lastUpdateTime != null) {
            builder.append("lastUpdateTime=");
            builder.append(lastUpdateTime);
            builder.append(", ");
        }
        if (email != null) {
            builder.append("email=");
            builder.append(email);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (nameSpace != null) {
            builder.append("nameSpace=");
            builder.append(nameSpace);
            builder.append(", ");
        }
        if (output != null) {
            builder.append("output=");
            builder.append(output);
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((executionId == null) ? 0 : executionId.hashCode());
        result = prime * result + ((finishTime == null) ? 0 : finishTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((lastUpdateTime == null) ? 0 : lastUpdateTime.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nameSpace == null) ? 0 : nameSpace.hashCode());
        result = prime * result + ((output == null) ? 0 : output.hashCode());
        result = prime * result + ((phase == null) ? 0 : phase.hashCode());
        result = prime * result + Float.floatToIntBits(progress);
        result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
        result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ProcessDescriptor)) {
            return false;
        }
        ProcessDescriptor other = (ProcessDescriptor) obj;
        if (clusterId == null) {
            if (other.clusterId != null) {
                return false;
            }
        } else if (!clusterId.equals(other.clusterId)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (executionId == null) {
            if (other.executionId != null) {
                return false;
            }
        } else if (!executionId.equals(other.executionId)) {
            return false;
        }
        if (finishTime == null) {
            if (other.finishTime != null) {
                return false;
            }
        } else if (!finishTime.equals(other.finishTime)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (lastUpdateTime == null) {
            if (other.lastUpdateTime != null) {
                return false;
            }
        } else if (!lastUpdateTime.equals(other.lastUpdateTime)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nameSpace == null) {
            if (other.nameSpace != null) {
                return false;
            }
        } else if (!nameSpace.equals(other.nameSpace)) {
            return false;
        }
        if (output == null) {
            if (other.output != null) {
                return false;
            }
        } else if (!output.equals(other.output)) {
            return false;
        }
        if (phase != other.phase) {
            return false;
        }
        if (Float.floatToIntBits(progress) != Float.floatToIntBits(other.progress)) {
            return false;
        }
        if (result == null) {
            if (other.result != null) {
                return false;
            }
        } else if (!result.equals(other.result)) {
            return false;
        }
        if (startTime == null) {
            if (other.startTime != null) {
                return false;
            }
        } else if (!startTime.equals(other.startTime)) {
            return false;
        }
        return true;
    }

}
