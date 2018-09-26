(ns shim.dev)

(def ast `((:pragmaDirective
             "pragma"
             (:pragmaName (:identifier "solidity"))
             (:pragmaValue
              (:version (:versionConstraint (:versionOperator "^") "0.4.21")))
             ";")
            (:contractDefinition
             "contract"
             (:identifier "PatternMatching")
             "{"
             (:contractPart
              (:enumDefinition
               "enum"
               (:identifier "Match")
               "{"
               (:enumValue (:identifier "One"))
               ","
               (:enumValue (:identifier "Two"))
               ","
               (:enumValue (:identifier "Three"))
               ","
               (:enumValue (:identifier "Four"))
               ","
               (:enumValue (:identifier "Error"))
               "}"))
             (:contractPart
              (:functionDefinition
               "function"
               (:identifier "test")
               (:parameterList "(" ")")
               (:modifierList)
               (:returnParameters
                "returns"
                (:parameterList
                 "("
                 (:parameter
                  (:typeName (:userDefinedTypeName (:identifier "Match"))))
                 ")"))
               (:block
                "{"
                (:statement
                 (:simpleStatement
                  (:variableDeclarationStatement
                   (:variableDeclaration
                    (:typeName (:elementaryTypeName "var"))
                    (:identifier "x"))
                   "="
                   (:expression (:primaryExpression "false"))
                   ";")))
                (:statement
                 (:simpleStatement
                  (:variableDeclarationStatement
                   (:variableDeclaration
                    (:typeName (:elementaryTypeName "var"))
                    (:identifier "y"))
                   "="
                   (:expression (:primaryExpression "true"))
                   ";")))
                (:statement
                 (:simpleStatement
                  (:variableDeclarationStatement
                   (:variableDeclaration
                    (:typeName (:elementaryTypeName "var"))
                    (:identifier "z"))
                   "="
                   (:expression (:primaryExpression "false"))
                   ";")))
                (:statement
                 (:simpleStatement
                  (:variableDeclarationStatement
                   (:variableDeclaration
                    (:typeName (:userDefinedTypeName (:identifier "Match")))
                    (:identifier "m"))
                   "="
                   (:expression
                    (:expression (:primaryExpression (:identifier "matches")))
                    "("
                    (:functionCallArguments
                     (:expressionList
                      (:expression
                       (:primaryExpression
                        (:tupleExpression
                         "["
                         (:expression (:primaryExpression (:identifier "x")))
                         ","
                         (:expression (:primaryExpression (:identifier "y")))
                         ","
                         (:expression (:primaryExpression (:identifier "z")))
                         "]")))
                      ","
                      (:expression
                       (:primaryExpression
                        (:tupleExpression
                         "["
                         (:expression (:primaryExpression (:identifier "_")))
                         ","
                         (:expression (:primaryExpression "false"))
                         ","
                         (:expression (:primaryExpression "true"))
                         "]")))
                      ","
                      (:expression
                       (:expression (:primaryExpression (:identifier "Match")))
                       "."
                       (:identifier "One"))
                      ","
                      (:expression
                       (:primaryExpression
                        (:tupleExpression
                         "["
                         (:expression (:primaryExpression "false"))
                         ","
                         (:expression (:primaryExpression "true"))
                         ","
                         (:expression (:primaryExpression (:identifier "_")))
                         "]")))
                      ","
                      (:expression
                       (:expression (:primaryExpression (:identifier "Match")))
                       "."
                       (:identifier "Two"))
                      ","
                      (:expression
                       (:primaryExpression
                        (:tupleExpression
                         "["
                         (:expression (:primaryExpression (:identifier "_")))
                         ","
                         (:expression (:primaryExpression (:identifier "_")))
                         ","
                         (:expression (:primaryExpression "false"))
                         "]")))
                      ","
                      (:expression
                       (:expression (:primaryExpression (:identifier "Match")))
                       "."
                       (:identifier "Three"))
                      ","
                      (:expression
                       (:primaryExpression
                        (:tupleExpression
                         "["
                         (:expression (:primaryExpression (:identifier "_")))
                         ","
                         (:expression (:primaryExpression (:identifier "_")))
                         ","
                         (:expression (:primaryExpression "true"))
                         "]")))
                      ","
                      (:expression
                       (:expression (:primaryExpression (:identifier "Match")))
                       "."
                       (:identifier "Four"))))
                    ")")
                   ";")))
                (:statement
                 (:returnStatement
                  "return"
                  (:expression (:primaryExpression (:identifier "m")))
                  ";"))
                "}")))
             "}")
            "<EOF>"))
