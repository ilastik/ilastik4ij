package org.ilastik.ilastik4ij.util;

import java.util.Vector;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;

public class IlastikBoxModel implements MutableComboBoxModel {

    Vector entries = new Vector();
    int index = -1;

    public Object getSelectedItem() {
        if (index >= 0) {
            return ((ComboBoxDimensions) entries.elementAt(index)).getEntry();
        } else {
            return "";
        }
    }

    public void setSelectedItem(Object anItem) {
        for (int i = 0; i < entries.size(); i++) {
            if (((ComboBoxDimensions) entries.elementAt(i)).getEntry().equals(anItem)) {
                index = i;
                break;
            }
        }
    }

    public int getSize() {
        return entries.size();
    }

    public Object getElementAt(int index) {
        return ((ComboBoxDimensions) entries.elementAt(index)).getEntry();
    }

    public void addElement(Object obj) {
        if (!entries.contains(obj)) {
            int i = 0;

            while (i < entries.size() && ((ComboBoxDimensions) obj).isValid() == "works") {
                i++;
            }

            entries.add(i, obj);
            if (index == -1) {
                index = 0;
            }
        }
    }

    public void removeElement(Object obj) {
        if (entries.contains(obj)) {
            entries.remove(obj);
        }
    }

    public void insertElementAt(Object obj, int index) {
        entries.add(index, obj);
    }

    public void removeElementAt(int index) {
        if (entries.size() > index) {
            entries.removeElementAt(index);
        }
    }

    public void addListDataListener(ListDataListener l) {
    }

    public void removeListDataListener(ListDataListener l) {
    }

}
