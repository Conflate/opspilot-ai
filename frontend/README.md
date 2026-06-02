# OpsPilot AI Frontend

This is the React/Vite operations console for OpsPilot AI. It talks to the Spring Boot backend through the Vite `/api` proxy.

## Run

```bash
npm install
npm run dev
```

Open:

```text
http://127.0.0.1:5173/
```

The backend should be running on:

```text
http://localhost:8080
```

## Scripts

```bash
npm run lint
npm run build
npm run preview
```

## Product Flow

- View dashboard metrics and ticket distribution
- Filter, search, sort, and select tickets
- Create operational tickets
- Run AI triage
- Approve or reject recommendations
- Apply manual triage after rejecting AI output
- Check possible duplicates and mark confirmed duplicates
- Review the audit trail for each ticket

## Design Notes

The UI is designed as an internal operations workspace, not a marketing page. It prioritizes fast scanning, stable panels, compact controls, visible workflow state, and clear separation between advisory AI output and human decisions.
