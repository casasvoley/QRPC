package com.mycompany.qrpc;

import java.util.Map;

public class PatternLogicModule {

    private final static int error = 20;

    public static String calculateAtomicPattern(Map<String,Double> targetInfo,
                                                Map<String,Double> referenceInfo) {

        // Comprobamos que no hay valores nulos en los mapas
        if (targetInfo.containsValue(null) || referenceInfo.containsValue(null)) return "";

        // Vector de dirección de nuestro dispositivo
        double[] v1 = new double[]{targetInfo.get("longitude_speed"),
                targetInfo.get("latitude_speed")};
        // Vector de dirección del dispositivo de referencia
        double[] v2 = new double[]{referenceInfo.get("longitude_speed"),
                referenceInfo.get("latitude_speed")};
        // Vector con origen en el dispositivo de referencia y que apunta hacia nuestro dispositivo
        double[] vd = new double[]{targetInfo.get("longitude")-referenceInfo.get("longitude"),
                targetInfo.get("latitude")-referenceInfo.get("latitude")};

        // Ángulo que debe rotar v1 para coincidir en dirección y sentido con vd
        double alpha = calculateAngle(vd,v1);
        // Ángulo que debe rotar v2 para coincidir en dirección y sentido con vd
        double beta = calculateAngle(vd,v2);

        // Signo de alpha
        int alpha_sign = calculateRotationSign(vd, v1);
        // Signo de beta
        int beta_sign = calculateRotationSign(vd, v2);

        // Si un ángulo es de 180º, cambiamos su signo a 180 si el otro ángulo es de 0º (para
        // que los signos no coincidan al ser los casos tipo ↕) o le damos el mismo signo
        // que el otro ángulo si este es <180º (para que los signos coincidan por ser los
        // casos tipo X^{0-} y X^{+0})
        if (anglesEqual(alpha,180)) {
            if (anglesEqual(beta,0)) {
                alpha_sign = 180;
            } else if (beta < 180) {
                alpha_sign = beta_sign;
            }
        }
        if (anglesEqual(beta,180)) {
            if (anglesEqual(alpha,0)) {
                beta_sign = 180;
            } else if (alpha < 180) {
                beta_sign = alpha_sign;
            }
        }

        // Inicio de la determinación del patrón
        if (alpha_sign == beta_sign) {
            // Tipos X^{++}, X^{--}, X^{0-}, X^{+0}, ↑↑, ↑

            if (anglesEqual(alpha,beta)) {
                // Tipos ↑↑, ↑

                if (anglesEqual(alpha,0)) {
                    // Resultado: tipo  ↑_+
                    return "↑_+"; // R.drawable.pattern_1;
                } else {
                    // Tipos ↑↑, ↑_-

                    if (anglesEqual(alpha,180)) {
                        // Resultado: tipo  ↑_-
                        return "↑_-"; // R.drawable.pattern_2;
                    } else {
                        // Tipos ↑↑

                        if (anglesEqual(alpha,90)) {
                            // Tipos ↑↑_{-0}, ↑↑_{+0}

                            if (beta_sign > 0){
                                // Resultado: tipo  ↑↑_{+0}
                                return "↑↑_{+0}"; // R.drawable.pattern_3;
                            } else {
                                // Resultado: tipo  ↑↑_{-0}
                                return "↑↑_{-0}"; // R.drawable.pattern_4;
                            }
                        } else {
                            // Tipos ↑↑_{--}, ↑↑_{+-}, ↑↑_{-+}, ↑↑_{++}

                            if (alpha > 90) {
                                // Tipos ↑↑_{--}, ↑↑_{+-}

                                if (beta_sign > 0){
                                    // Resultado: tipo  ↑↑_{+-}
                                    return "↑↑_{+-}"; // R.drawable.pattern_5;
                                } else {
                                    // Resultado: tipo  ↑↑_{--}
                                    return "↑↑_{--}"; // R.drawable.pattern_6;
                                }
                            } else {
                                // Tipos ↑↑_{-+}, ↑↑_{++}

                                if (beta_sign > 0){
                                    // Resultado: tipo  ↑↑_{++}
                                    return "↑↑_{++}"; // R.drawable.pattern_7;
                                } else {
                                    // Resultado: tipo  ↑↑_{-+}
                                    return "↑↑_{-+}"; // R.drawable.pattern_8;
                                }
                            }
                        }
                    }
                }
            } else {
                // Tipos X^{++}, X^{--}, X^{0-}, X^{+0}

                if (alpha > beta) {
                    // Tipos X^{++}, X^{+0}

                    if (anglesEqual(alpha,180)) {
                        // Tipos X^{+0}

                        if (beta_sign > 0) {
                            // Resultado: tipo  X_+^{+0}
                            return "X_+^{+0}"; // R.drawable.pattern_9;
                        } else {
                            // Resultado: tipo  X_-^{+0}
                            return "X_-^{+0}"; // R.drawable.pattern_10;
                        }
                    } else {
                        // Tipos X^{++}

                        if (anglesEqual(alpha + beta,180)) {
                            // Tipos X_{+0}^{++}, X_{-0}^{++}

                            if (beta_sign > 0) {
                                // Resultado: tipo  X_{+0}^{++}
                                return "X_{+0}^{++}"; // R.drawable.pattern_11;
                            } else {
                                // Resultado: tipo  X_{-0}^{++}
                                return "X_{-0}^{++}"; // R.drawable.pattern_12;
                            }
                        } else {
                            // Tipos X_{++}^{++}, X_{+-}^{++}, X_{-+}^{++}, X_{--}^{++}

                            if (alpha + beta > 180) {
                                // Tipos X_{--}^{++}, X_{+-}^{++}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{+-}^{++}
                                    return "X_{+-}^{++}"; // R.drawable.pattern_13;
                                } else {
                                    // Resultado: tipo  X_{--}^{++}
                                    return "X_{--}^{++}"; // R.drawable.pattern_14;
                                }
                            } else {
                                // Tipos X_{-+}^{++}, X_{++}^{++}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{++}^{++}
                                    return "X_{++}^{++}"; // R.drawable.pattern_15;
                                } else {
                                    // Resultado: tipo  X_{-+}^{++}
                                    return "X_{-+}^{++}"; // R.drawable.pattern_16;
                                }
                            }
                        }
                    }
                } else {
                    // Tipos X^{--}, X^{0-}

                    if (anglesEqual(beta,180)) {
                        // Tipos X^{0-}

                        if (beta_sign > 0) {
                            // Resultado: tipo  X_{-|+-}^{0-}
                            return "X_{-|+-}^{0-}"; // R.drawable.pattern_17;
                        } else {
                            // Resultado: tipo  X_{+|--}^{0-}
                            return "X_{+|--}^{0-}"; // R.drawable.pattern_18;
                        }
                    } else {
                        // Tipos X^{--}

                        if (anglesEqual(alpha + beta,180)) {
                            // Tipos X_{+0}^{--}, X_{-0}^{--}

                            if (beta_sign > 0) {
                                // Resultado: tipo  X_{+0}^{--}
                                return "X_{+0}^{--}"; // R.drawable.pattern_19;
                            } else {
                                // Resultado: tipo  X_{-0}^{--}
                                return "X_{-0}^{--}"; // R.drawable.pattern_20;
                            }
                        } else {
                            // Tipos X_{++}^{--}, X_{+-}^{--}, X_{-+}^{--}, X_{--}^{--}

                            if (alpha + beta > 180) {
                                // Tipos X_{--}^{--}, X_{+-}^{--}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{+-}^{--}
                                    return "X_{+-}^{--}"; // R.drawable.pattern_21;
                                } else {
                                    // Resultado: tipo  X_{--}^{--}
                                    return "X_{--}^{--}"; // R.drawable.pattern_22;
                                }
                            } else {
                                // Tipos X_{-+}^{--}, X_{++}^{--}
                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{++}^{--}
                                    return "X_{++}^{--}"; // R.drawable.pattern_23;
                                } else {
                                    // Resultado: tipo  X_{-+}^{--}
                                    return "X_{-+}^{--}"; // R.drawable.pattern_24;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tipos X^{-+}, X^{+-}, X^{0+}, X^{-0}, ↑↓, ↕

            if (anglesEqual(alpha + beta,180)) {
                // Tipos ↑↓, ↕

                if (anglesEqual(alpha,beta)) {
                    // Tipos ↑↓_{-0}, ↑↓_{+0}

                    if (beta_sign > 0) {
                        // Resultado: tipo  ↑↓_{+0}
                        return "↑↓_{+0}"; // R.drawable.pattern_25;
                    } else {
                        // Resultado: tipo  ↑↓_{-0}
                        return "↑↓_{-0}"; // R.drawable.pattern_26;
                    }
                } else {
                    // Tipos ↑↓_{±±}, ↕

                    if (alpha > beta) {
                        // Tipos ↑↓_{±+}, ↕_+

                        if (beta_sign == 0) {
                            // Resultado: tipo  ↕_+
                            return "↕_+"; // R.drawable.pattern_27;
                        } else {
                            // Tipos ↑↓_{±+}

                            if (beta_sign > 0) {
                                // Resultado: tipo  ↑↓_{++}
                                return "↑↓_{++}"; // R.drawable.pattern_28;
                            } else {
                                // Resultado: tipo  ↑↓_{-+}
                                return "↑↓_{-+}"; // R.drawable.pattern_29;
                            }
                        }
                    } else {
                        // Tipos ↑↓_{±-}, ↕_-

                        if (alpha_sign == 0) {
                            // Resultado: tipo  ↕_-
                            return "↕_-"; // R.drawable.pattern_30;
                        } else {
                            // Tipos ↑↓_{±-}

                            if (beta_sign > 0) {
                                // Resultado: tipo  ↑↓_{+-}
                                return "↑↓_{+-}"; // R.drawable.pattern_31;
                            } else {
                                // Resultado: tipo  ↑↓_{--}
                                return "↑↓_{--}"; // R.drawable.pattern_32;
                            }
                        }
                    }
                }
            } else {
                // Tipos X^{-+}, X^{+-}, X^{0+}, X^{-0}

                if (alpha + beta > 180) {
                    // Tipos X^{+-}

                    if (anglesEqual(alpha,beta)) {
                        // Tipos X_{-0}^{+-}, X_{+0}^{+-}

                        if (beta_sign > 0) {
                            // Resultado: tipo  X_{+0}^{+-}
                            return "X_{+0}^{+-}"; // R.drawable.pattern_33;
                        } else {
                            // Resultado: tipo  X_{-0}^{+-}
                            return "X_{-0}^{+-}"; // R.drawable.pattern_34;
                        }
                    } else {
                        // Tipos X_{±±}^{+-}

                        if (alpha > beta) {
                            // Tipos X_{-+}^{+-}, X_{++}^{+-}

                            if (beta_sign > 0) {
                                // Resultado: tipo  X_{++}^{+-}
                                return "X_{++}^{+-}"; // R.drawable.pattern_35;
                            } else {
                                // Resultado: tipo  X_{-+}^{+-}
                                return "X_{-+}^{+-}"; // R.drawable.pattern_36;
                            }
                        } else {
                            // Tipos X_{--}^{+-}, X_{+-}^{+-}

                            if (beta_sign > 0) {
                                // Resultado: tipo  X_{+-}^{+-}
                                return "X_{+-}^{+-}"; // R.drawable.pattern_37;
                            } else {
                                // Resultado: tipo  X_{--}^{+-}
                                return "X_{--}^{+-}"; // R.drawable.pattern_38;
                            }
                        }
                    }
                } else {
                    // Tipos X^{-+}, X^{0+}, X^{-0}

                    if (anglesEqual(alpha,beta)) {
                        // Tipos X_{-0}^{-+}, X_{+0}^{-+}

                        if (beta_sign > 0) {
                            // Resultado: tipo  X_{+0}^{-+}
                            return "X_{+0}^{-+}"; // R.drawable.pattern_39;
                        } else {
                            // Resultado: tipo  X_{-0}^{-+}
                            return "X_{-0}^{-+}"; // R.drawable.pattern_40;
                        }
                    } else {
                        // Tipos X_{±±}^{-+}, X^{0+}, X^{-0}

                        if (alpha > beta) {
                            // Tipos X_{-+}^{-+}, X_{++}^{-+}, X^{0+}

                            if (beta_sign == 0) {
                                // Tipos X^{0+}

                                if (alpha_sign > 0) {
                                    // Resultado: tipo  X_{+|-+}^{0+}
                                    return "X_{+|-+}^{0+}"; // R.drawable.pattern_41;
                                } else {
                                    // Resultado: tipo  X_{-|++}^{0+}
                                    return "X_{-|++}^{0+}"; // R.drawable.pattern_42;
                                }
                            } else {
                                // Tipos X_{-+}^{-+}, X_{++}^{-+}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{++}^{-+}
                                    return "X_{++}^{-+}"; // R.drawable.pattern_43;
                                } else {
                                    // Resultado: tipo  X_{-+}^{-+}
                                    return "X_{-+}^{-+}"; // R.drawable.pattern_44;
                                }
                            }
                        } else {
                            // Tipos X_{--}^{-+}, X_{+-}^{-+}, X^{-0}

                            if (alpha_sign == 0) {
                                // Tipos X^{-0}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_+^{-0}
                                    return "X_+^{-0}"; // R.drawable.pattern_45;
                                } else {
                                    // Resultado: tipo  X_-^{-0}
                                    return "X_-^{-0}"; // R.drawable.pattern_46;
                                }
                            } else {
                                // Tipos X_{--}^{-+}, X_{+-}^{-+}

                                if (beta_sign > 0) {
                                    // Resultado: tipo  X_{+-}^{-+}
                                    return "X_{+-}^{-+}"; // R.drawable.pattern_47;
                                } else {
                                    // Resultado: tipo  X_{--}^{-+}
                                    return "X_{--}^{-+}"; // R.drawable.pattern_48;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cálcula el ángulo entre dos vectores
    private static double calculateAngle(double[] vector_1, double[] vector_2) {
        return Math.acos( (vector_1[0]*vector_2[0] + vector_1[1]*vector_2[1])
                / (Math.sqrt(Math.pow(vector_1[0],2) + Math.pow(vector_1[1],2))
                * Math.sqrt(Math.pow(vector_2[0],2) + Math.pow(vector_2[1],2))) )
                * 180 / Math.PI;
    }

    // Cálcula el sentido del ángulo entre dos vectores
    private static int calculateRotationSign(double[] vector_1, double[] vector_2) {
        // Sólo calculamos la tercera coordenada del producto vectorial, ya que es la que nos
        // interesa (las otras dos son nulas)
        double z_coordinate = (vector_1[0]*vector_2[1] - vector_1[1]*vector_2[0]);
        if (z_coordinate > 0) {
            return 1; // Giro hacia la derecha
        } else if (z_coordinate < 0) {
            return -1; // Giro hacia la izquierda
        } else {
            return 0; // Giro de 0ª o de 180ª
        }
    }

    // Devuelve true si dos ángulos son iguales salvo un cierto margen de error
    private static boolean anglesEqual(double angle_1, double angle_2) {
        if (Math.abs(angle_1-angle_2) < error) {
            return true;
        } else {
            return false;
        }
    }
}
