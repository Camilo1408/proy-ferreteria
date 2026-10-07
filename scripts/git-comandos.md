# Comandos de gestión del repositorio

Flujo de trabajo: ramas cortas + pull request; `main` siempre debe estar verde (el pipeline despliega desde `main`).

```bash
# Clonar y configurar
git clone https://github.com/Camilo1408/proy-ferreteria.git
cd proy-ferreteria

# Nueva tarea (rama)
git switch -c feat/nombre-corto

# Guardar cambios (mensajes: feat, fix, test, docs, chore)
git add -A
git commit -m "feat: descripción breve"

# Subir y abrir PR
git push -u origin feat/nombre-corto
gh pr create --fill

# Actualizar tu rama con main
git fetch origin && git rebase origin/main

# Ver estado del pipeline y logs de un fallo
gh run list --limit 5
gh run view --log-failed

# Issues y tareas
gh issue list --label ci-fallo
gh issue create --title "Tarea: ..." --label tarea --assignee @me

# Deshacer sin perder historial
git revert <commit>
```
