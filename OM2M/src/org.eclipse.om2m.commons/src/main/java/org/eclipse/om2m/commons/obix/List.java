package org.eclipse.om2m.commons.obix;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
/**
 * List oBIX object
 * @author Francois Aissaoui
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "list", propOrder = {
    "objGroup"
})
@XmlRootElement
public class List extends Obj{

    @XmlElements({
        @XmlElement(name = "obj", type = Obj.class),
        @XmlElement(name = "bool", type = Bool.class),
        @XmlElement(name = "int", type = Int.class),
        @XmlElement(name = "real", type = Real.class),
        @XmlElement(name = "str", type = Str.class),
        @XmlElement(name = "enum", type = Enum.class),
        @XmlElement(name = "abstime", type = Abstime.class),
        @XmlElement(name = "reltime", type = Reltime.class),
        @XmlElement(name = "date", type = Date.class),
        @XmlElement(name = "time", type = Time.class),
        @XmlElement(name = "uri", type = Uri.class),
        @XmlElement(name = "list", type = List.class),
        @XmlElement(name = "ref", type = Ref.class),
        @XmlElement(name = "err", type = Err.class),
        @XmlElement(name = "op", type = Op.class),
        @XmlElement(name = "feed", type = Feed.class)
    })
    protected java.util.List<Object> objGroup;
    @XmlAttribute(name = "of")
    protected String of;
    @XmlAttribute(name = "min")
    protected Integer min;
    @XmlAttribute(name = "max")
    protected Integer max;
    @XmlAttribute(name = "displayName")
    protected String displayName;
    @XmlAttribute(name = "display")
    protected String display;
    @XmlAttribute(name = "icon")
    @XmlSchemaType(name = "anyURI")
    protected String icon;
    @XmlAttribute(name = "precision")
    protected Integer precision;
    @XmlAttribute(name = "status")
    protected Status status;
    @XmlAttribute(name = "unit")
    protected String unit;
    @XmlAttribute(name = "writable")
    protected Boolean writable;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "href")
    protected String href;
    @XmlAttribute(name = "null")
    protected Boolean _null;

    public List(){}
    /**
     * Gets the value of the objGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the objGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getObjGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Obj }
     * {@link Bool }
     * {@link Int }
     * {@link Real }
     * {@link Str }
     * {@link Enum }
     * {@link Abstime }
     * {@link Reltime }
     * {@link Date }
     * {@link Time }
     * {@link Uri }
     * {@link List }
     * {@link Ref }
     * {@link Err }
     * {@link Op }
     * {@link Feed }
     * 
     * 
     */
    public java.util.List<Object> getObjGroup() {
        if (objGroup == null) {
            objGroup = new ArrayList<Object>();
        }
        return this.objGroup;
    }

    /**
     * Obtient la valeur de la propri??t?? of.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOf() {
        return of;
    }

    /**
     * D??finit la valeur de la propri??t?? of.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOf(String value) {
        this.of = value;
    }

    /**
     * Obtient la valeur de la propri??t?? min.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMin() {
        return min;
    }

    /**
     * D??finit la valeur de la propri??t?? min.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMin(Integer value) {
        this.min = value;
    }

    /**
     * Obtient la valeur de la propri??t?? max.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMax() {
        return max;
    }

    /**
     * D??finit la valeur de la propri??t?? max.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMax(Integer value) {
        this.max = value;
    }

    /**
     * Obtient la valeur de la propri??t?? displayName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * D??finit la valeur de la propri??t?? displayName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisplayName(String value) {
        this.displayName = value;
    }

    /**
     * Obtient la valeur de la propri??t?? display.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisplay() {
        return display;
    }

    /**
     * D??finit la valeur de la propri??t?? display.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisplay(String value) {
        this.display = value;
    }

    /**
     * Obtient la valeur de la propri??t?? icon.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIcon() {
        return icon;
    }

    /**
     * D??finit la valeur de la propri??t?? icon.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIcon(String value) {
        this.icon = value;
    }

    /**
     * Obtient la valeur de la propri??t?? precision.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPrecision() {
        return precision;
    }

    /**
     * D??finit la valeur de la propri??t?? precision.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPrecision(Integer value) {
        this.precision = value;
    }

    /**
     * Obtient la valeur de la propri??t?? status.
     * 
     * @return
     *     possible object is
     *     {@link Status }
     *     
     */
    public Status getStatus() {
        return status;
    }

    /**
     * D??finit la valeur de la propri??t?? status.
     * 
     * @param value
     *     allowed object is
     *     {@link Status }
     *     
     */
    public void setStatus(Status value) {
        this.status = value;
    }

    /**
     * Obtient la valeur de la propri??t?? unit.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnit() {
        return unit;
    }

    /**
     * D??finit la valeur de la propri??t?? unit.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     * Obtient la valeur de la propri??t?? writable.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWritable() {
        return writable;
    }

    /**
     * D??finit la valeur de la propri??t?? writable.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWritable(Boolean value) {
        this.writable = value;
    }

    /**
     * Obtient la valeur de la propri??t?? name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * D??finit la valeur de la propri??t?? name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Obtient la valeur de la propri??t?? href.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHref() {
        return href;
    }

    /**
     * D??finit la valeur de la propri??t?? href.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Obtient la valeur de la propri??t?? null.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isNull() {
        return _null;
    }

    /**
     * D??finit la valeur de la propri??t?? null.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setNull(Boolean value) {
        this._null = value;
    }

}
