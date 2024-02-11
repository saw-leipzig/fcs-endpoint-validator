/**
 * This software is copyright (c) 2013-2016 by
 *  - Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 * This is free software. You can redistribute it
 * and/or modify it under the terms described in
 * the GNU General Public License v3 of which you
 * should have received a copy. Otherwise you can download
 * it from
 *
 *   http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * @copyright Institut fuer Deutsche Sprache (http://www.ids-mannheim.de)
 *
 * @license http://www.gnu.org/licenses/gpl-3.0.txt
 *  GNU General Public License v3
 */
package eu.clarin.sru.fcs.tester;

public enum FCSTestProfile {
    CLARIN_FCS_1_0(0),
    CLARIN_FCS_2_0(1),
    CLARIN_FCS_LEGACY(2);

    final int id;

    private FCSTestProfile(final int id) {
        this.id = id;
    }

    public String toDisplayString() {
        switch (this) {
            case CLARIN_FCS_1_0:
                return "CLARIN FCS 1.0";
            case CLARIN_FCS_2_0:
                return "CLARIN FCS 2.0";
            case CLARIN_FCS_LEGACY:
                return "Legacy FCS";
            default:
                return "";
        }
    }

} // enum FCSTestProfile
