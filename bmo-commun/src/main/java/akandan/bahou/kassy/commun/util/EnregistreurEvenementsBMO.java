package akandan.bahou.kassy.commun.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnregistreurEvenementsBMO {

    private EnregistreurEvenementsBMO() {
    }

    public static Logger getLogger(Class<?> classe) {
        return LoggerFactory.getLogger(classe);
    }
}