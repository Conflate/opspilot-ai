import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const emptyDashboard = {
  totalTickets: 0,
  openTickets: 0,
  pendingReviewTickets: 0,
  approvedTickets: 0,
  rejectedTickets: 0,
  criticalTickets: 0,
  averageConfidenceScore: 0,
  ticketsBySeverity: {},
  ticketsByCategory: {},
}

const initialForm = {
  title: '',
  description: '',
  sourceSystem: 'Service Desk',
  affectedService: '',
}

const initialReviewForm = {
  reviewer: '',
  reviewNote: '',
}

const initialManualTriageForm = {
  reviewer: '',
  severity: 'MEDIUM',
  category: 'OTHER',
  probableCause: '',
  recommendedAction: '',
}

const severityOrder = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNTRIAGED']
const categoryOrder = [
  'ACCESS',
  'PERFORMANCE',
  'CONFIGURATION',
  'BUG',
  'INFRASTRUCTURE',
  'USER_SUPPORT',
  'SECURITY',
  'DATA_ISSUE',
  'OTHER',
  'UNCLASSIFIED',
]

const ticketFilterOptions = [
  { key: 'ATTENTION', label: 'Attention' },
  { key: 'ALL', label: 'All' },
  { key: 'OPEN', label: 'Open' },
  { key: 'PENDING_REVIEW', label: 'Review' },
  { key: 'APPROVED', label: 'Approved' },
  { key: 'REJECTED', label: 'Rejected' },
]

const ticketSortOptions = [
  { key: 'NEWEST', label: 'Newest' },
  { key: 'OLDEST', label: 'Oldest' },
  { key: 'SEVERITY', label: 'Severity' },
  { key: 'STATUS', label: 'Status' },
]

const severityRank = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
  UNTRIAGED: 4,
}

const statusRank = {
  PENDING_REVIEW: 0,
  OPEN: 1,
  APPROVED: 2,
  REJECTED: 3,
}

const selectedTicketStorageKey = 'opspilot:selectedTicketId'

function App() {
  const [dashboard, setDashboard] = useState(emptyDashboard)
  const [tickets, setTickets] = useState([])
  const [selectedTicketId, setSelectedTicketId] = useState(readStoredSelectedTicketId)
  const [ticketFilter, setTicketFilter] = useState('ATTENTION')
  const [ticketSearch, setTicketSearch] = useState('')
  const [ticketSort, setTicketSort] = useState('NEWEST')
  const [triageResults, setTriageResults] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [duplicateResults, setDuplicateResults] = useState({ ticketId: null, candidates: [] })
  const [duplicateLinks, setDuplicateLinks] = useState([])
  const [form, setForm] = useState(initialForm)
  const [reviewForm, setReviewForm] = useState(initialReviewForm)
  const [manualTriageForm, setManualTriageForm] = useState(initialManualTriageForm)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isTriaging, setIsTriaging] = useState(false)
  const [isReviewing, setIsReviewing] = useState('')
  const [isSubmittingManualTriage, setIsSubmittingManualTriage] = useState(false)
  const [isCheckingDuplicates, setIsCheckingDuplicates] = useState(false)
  const [isMarkingDuplicate, setIsMarkingDuplicate] = useState('')
  const [isLoadingContext, setIsLoadingContext] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const selectedTicket = useMemo(
    () => tickets.find((ticket) => ticket.id === selectedTicketId) ?? tickets[0],
    [selectedTicketId, tickets],
  )
  const latestTriage = triageResults[0]
  const duplicateCandidates =
    duplicateResults.ticketId === selectedTicket?.id ? duplicateResults.candidates : []
  const confirmedDuplicateIds = useMemo(
    () => new Set(duplicateLinks.map((link) => link.duplicateOfTicketId)),
    [duplicateLinks],
  )
  const canReview = selectedTicket?.status === 'PENDING_REVIEW' && Boolean(latestTriage)
  const canSubmitReview = canReview && Boolean(reviewForm.reviewer.trim()) && !isReviewing
  const canSubmitManualTriage =
    selectedTicket?.status === 'REJECTED' &&
    Boolean(manualTriageForm.reviewer.trim()) &&
    Boolean(manualTriageForm.probableCause.trim()) &&
    Boolean(manualTriageForm.recommendedAction.trim()) &&
    !isSubmittingManualTriage
  const filterCounts = useMemo(() => getFilterCounts(tickets), [tickets])
  const filteredTickets = useMemo(
    () =>
      sortTickets(
        tickets.filter(
          (ticket) =>
            matchesTicketFilter(ticket, ticketFilter) && matchesTicketSearch(ticket, ticketSearch),
        ),
        ticketSort,
      ),
    [ticketFilter, ticketSearch, ticketSort, tickets],
  )

  const request = useCallback(async (path, options) => {
    const response = await fetch(path, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    })

    if (!response.ok) {
      let message = `${response.status} ${response.statusText}`
      try {
        const body = await response.json()
        message = body.message || message
      } catch {
        // Keep the HTTP status message when the response is not JSON.
      }
      throw new Error(message)
    }

    if (response.status === 204) {
      return null
    }

    return response.json()
  }, [])

  const loadWorkspace = useCallback(async (preferredTicketId) => {
    setIsLoading(true)
    setError('')
    setNotice('')

    try {
      const [dashboardResponse, ticketResponse] = await Promise.all([
        request('/api/dashboard'),
        request('/api/tickets'),
      ])

      setDashboard(dashboardResponse)
      setTickets(ticketResponse)
      setSelectedTicketId((currentId) => {
        const nextId = resolveSelectedTicketId(ticketResponse, preferredTicketId ?? currentId)
        storeSelectedTicketId(nextId)
        return nextId
      })
    } catch (exception) {
      setError(`Unable to load OpsPilot data: ${exception.message}`)
    } finally {
      setIsLoading(false)
    }
  }, [request])

  const loadTicketContext = useCallback(async (ticketId) => {
    setIsLoadingContext(true)
    setError('')

    try {
      const [triageResponse, auditResponse, duplicateLinkResponse] = await Promise.all([
        request(`/api/tickets/${ticketId}/triage`),
        request(`/api/tickets/${ticketId}/audit-logs`),
        request(`/api/tickets/${ticketId}/duplicates/links`),
      ])

      setTriageResults(triageResponse)
      setAuditLogs(auditResponse)
      setDuplicateLinks(duplicateLinkResponse)
    } catch (exception) {
      setError(`Unable to load ticket activity: ${exception.message}`)
    } finally {
      setIsLoadingContext(false)
    }
  }, [request])

  useEffect(() => {
    Promise.resolve().then(loadWorkspace)
  }, [loadWorkspace])

  useEffect(() => {
    if (selectedTicket?.id) {
      storeSelectedTicketId(selectedTicket.id)
      Promise.resolve().then(() => loadTicketContext(selectedTicket.id))
    }
  }, [loadTicketContext, selectedTicket?.id])

  async function createTicket(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setError('')
    setNotice('')

    try {
      const ticket = await request('/api/tickets', {
        method: 'POST',
        body: JSON.stringify(form),
      })

      setForm(initialForm)
      setTicketFilter('OPEN')
      setTicketSearch('')
      setTicketSort('NEWEST')
      setSelectedTicketId(ticket.id)
      await loadWorkspace(ticket.id)
      setNotice(`Ticket #${ticket.id} created.`)
    } catch (exception) {
      setError(`Unable to create ticket: ${exception.message}`)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function runTriage() {
    if (!selectedTicket?.id) {
      return
    }

    setIsTriaging(true)
    setError('')
    setNotice('')

    try {
      await request(`/api/tickets/${selectedTicket.id}/triage`, { method: 'POST' })
      await loadWorkspace()
      await loadTicketContext(selectedTicket.id)
      setNotice('AI triage completed. Ticket is ready for review.')
    } catch (exception) {
      setError(`Unable to run AI triage: ${exception.message}`)
    } finally {
      setIsTriaging(false)
    }
  }

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function updateReviewField(field, value) {
    setReviewForm((current) => ({ ...current, [field]: value }))
  }

  function updateManualTriageField(field, value) {
    setManualTriageForm((current) => ({ ...current, [field]: value }))
  }

  async function submitReview(decision) {
    if (!selectedTicket?.id || !latestTriage?.id) {
      return
    }

    setIsReviewing(decision)
    setError('')
    setNotice('')

    try {
      await request(`/api/tickets/${selectedTicket.id}/triage/${latestTriage.id}/${decision}`, {
        method: 'POST',
        body: JSON.stringify(reviewForm),
      })

      setReviewForm(initialReviewForm)
      if (decision === 'reject' && latestTriage) {
        setManualTriageForm({
          reviewer: reviewForm.reviewer,
          severity: latestTriage.severity || 'MEDIUM',
          category: latestTriage.category || 'OTHER',
          probableCause: latestTriage.probableCause || '',
          recommendedAction: latestTriage.recommendedAction || '',
        })
      }
      await loadWorkspace()
      await loadTicketContext(selectedTicket.id)
      setNotice(`Recommendation ${decision === 'approve' ? 'approved' : 'rejected'}.`)
    } catch (exception) {
      setError(`Unable to ${decision} recommendation: ${exception.message}`)
    } finally {
      setIsReviewing('')
    }
  }

  async function checkDuplicates() {
    if (!selectedTicket?.id) {
      return
    }

    setIsCheckingDuplicates(true)
    setError('')
    setNotice('')

    try {
      const candidates = await request(`/api/tickets/${selectedTicket.id}/duplicates`)
      setDuplicateResults({ ticketId: selectedTicket.id, candidates })
      await loadTicketContext(selectedTicket.id)
      setNotice(`Duplicate check completed: ${candidates.length} candidates found.`)
    } catch (exception) {
      setError(`Unable to check duplicates: ${exception.message}`)
    } finally {
      setIsCheckingDuplicates(false)
    }
  }

  function selectDuplicate(ticketId) {
    setTicketFilter('ALL')
    setTicketSearch('')
    setSelectedTicketId(ticketId)
  }

  async function markDuplicate(candidate) {
    if (!selectedTicket?.id) {
      return
    }

    setIsMarkingDuplicate(candidate.ticketId)
    setError('')
    setNotice('')

    try {
      await request(`/api/tickets/${selectedTicket.id}/duplicates/${candidate.ticketId}`, {
        method: 'POST',
        body: JSON.stringify({
          markedBy: 'operator',
          note: candidate.reason,
        }),
      })

      await loadTicketContext(selectedTicket.id)
      setNotice(`Ticket #${selectedTicket.id} marked as duplicate of #${candidate.ticketId}.`)
    } catch (exception) {
      setError(`Unable to mark duplicate: ${exception.message}`)
    } finally {
      setIsMarkingDuplicate('')
    }
  }

  async function submitManualTriage(event) {
    event.preventDefault()

    if (!selectedTicket?.id) {
      return
    }

    setIsSubmittingManualTriage(true)
    setError('')
    setNotice('')

    try {
      await request(`/api/tickets/${selectedTicket.id}/triage/manual`, {
        method: 'POST',
        body: JSON.stringify(manualTriageForm),
      })

      setManualTriageForm(initialManualTriageForm)
      await loadWorkspace(selectedTicket.id)
      await loadTicketContext(selectedTicket.id)
      setNotice('Manual triage applied. Ticket moved to in progress.')
    } catch (exception) {
      setError(`Unable to apply manual triage: ${exception.message}`)
    } finally {
      setIsSubmittingManualTriage(false)
    }
  }

  const canCreateTicket = form.title.trim() && form.description.trim() && form.sourceSystem.trim()

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">OP</span>
          <div>
            <p className="eyebrow">OpsPilot AI</p>
            <h1>Operations Triage</h1>
          </div>
        </div>
        <div className="topbar-actions">
          <span className="connection-pill">Local workspace</span>
          <button className="secondary-button" type="button" onClick={() => loadWorkspace()}>
            Refresh
          </button>
        </div>
      </header>

      {error ? <div className="alert">{error}</div> : null}
      {notice ? <div className="notice">{notice}</div> : null}

      <section className="summary-grid" aria-label="Dashboard summary">
        <Metric label="Total" value={dashboard.totalTickets} />
        <Metric label="Open" value={dashboard.openTickets} />
        <Metric label="Pending review" value={dashboard.pendingReviewTickets} />
        <Metric label="Critical" value={dashboard.criticalTickets} intent="danger" />
        <Metric label="AI confidence" value={`${dashboard.averageConfidenceScore ?? 0}`} />
      </section>

      <section className="workspace">
        <div className="panel ticket-list-panel">
          <div className="panel-header">
            <div>
              <h2>Tickets</h2>
              <p>
                {isLoading
                  ? 'Loading tickets'
                  : `${filteredTickets.length} of ${tickets.length} records`}
              </p>
            </div>
          </div>

          <div className="ticket-filters" aria-label="Ticket filters">
            {ticketFilterOptions.map((option) => (
              <button
                className={`filter-button ${ticketFilter === option.key ? 'is-selected' : ''}`}
                key={option.key}
                type="button"
                onClick={() => setTicketFilter(option.key)}
              >
                <span>{option.label}</span>
                <strong>{filterCounts[option.key] ?? 0}</strong>
              </button>
            ))}
          </div>

          <div className="ticket-tools">
            <label>
              Search
              <input
                value={ticketSearch}
                onChange={(event) => setTicketSearch(event.target.value)}
                placeholder="Title, service, source"
              />
            </label>
            <label>
              Sort
              <select value={ticketSort} onChange={(event) => setTicketSort(event.target.value)}>
                {ticketSortOptions.map((option) => (
                  <option key={option.key} value={option.key}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="ticket-list">
            {filteredTickets.map((ticket) => (
              <button
                className={`ticket-row severity-${ticket.severity?.toLowerCase()} ${ticket.id === selectedTicket?.id ? 'is-selected' : ''}`}
                key={ticket.id}
                type="button"
                onClick={() => setSelectedTicketId(ticket.id)}
              >
                <span className="ticket-title">{ticket.title}</span>
                <span className="ticket-meta">
                  #{ticket.id} · {ticket.affectedService || 'No service'}
                </span>
                <span className="row-badges">
                  <Badge label={ticket.status} />
                  <Badge label={ticket.severity} tone={severityTone(ticket.severity)} />
                </span>
              </button>
            ))}

            {!isLoading && tickets.length === 0 ? (
              <p className="empty-state">No tickets yet. Create one to start triage.</p>
            ) : null}

            {!isLoading && tickets.length > 0 && filteredTickets.length === 0 ? (
              <p className="empty-state">No tickets match this queue.</p>
            ) : null}
          </div>
        </div>

        <div className="panel detail-panel">
          {selectedTicket ? (
            <>
              <div className="panel-header detail-header">
                <div>
                  <h2>{selectedTicket.title}</h2>
                  <p>
                    #{selectedTicket.id} · {formatDate(selectedTicket.createdAt)}
                  </p>
                </div>
                <div className="detail-status">
                  <Badge label={selectedTicket.status} />
                  <Badge label={selectedTicket.severity} tone={severityTone(selectedTicket.severity)} />
                </div>
              </div>

              <p className="description">{selectedTicket.description}</p>

              <div className="facts">
                <Fact label="Status" value={selectedTicket.status} />
                <Fact label="Severity" value={selectedTicket.severity} />
                <Fact label="Category" value={selectedTicket.category} />
                <Fact label="Source" value={selectedTicket.sourceSystem} />
              </div>

              <div className="workflow-banner">
                <div>
                  <span>Workflow</span>
                  <strong>{workflowMessage(selectedTicket, latestTriage, duplicateLinks)}</strong>
                </div>
                <small className={isLoadingContext ? 'activity-state is-loading' : 'activity-state'}>
                  {isLoadingContext ? 'Refreshing activity' : 'Activity current'}
                </small>
              </div>

              <div className="workflow-steps" aria-label="Ticket workflow">
                <WorkflowStep label="Details" status="complete" />
                <WorkflowStep label="Triage" status={latestTriage ? 'complete' : 'current'} />
                <WorkflowStep
                  label="Duplicates"
                  status={
                    duplicateLinks.length
                      ? 'complete'
                      : duplicateResults.ticketId === selectedTicket.id
                        ? 'current'
                        : 'pending'
                  }
                />
                <WorkflowStep
                  label="Decision"
                  status={decisionStepStatus(selectedTicket, canReview)}
                />
              </div>

              <div className="detail-workspace">
                <div className="detail-main">
                  <section className="detail-card decision-section">
                    <div className="section-heading">
                      <div>
                        <h3>Decision</h3>
                        <p>{decisionMessage(selectedTicket, latestTriage)}</p>
                      </div>
                    </div>

                    {selectedTicket.status === 'REJECTED' ? (
                      <form className="review-form manual-triage-form" onSubmit={submitManualTriage}>
                        <label>
                          Reviewer
                          <input
                            value={manualTriageForm.reviewer}
                            onChange={(event) => updateManualTriageField('reviewer', event.target.value)}
                            placeholder="Operator"
                          />
                        </label>
                        <div className="form-grid">
                          <label>
                            Severity
                            <select
                              value={manualTriageForm.severity}
                              onChange={(event) => updateManualTriageField('severity', event.target.value)}
                            >
                              {severityOrder
                                .filter((severity) => severity !== 'UNTRIAGED')
                                .map((severity) => (
                                  <option key={severity} value={severity}>
                                    {severity}
                                  </option>
                                ))}
                            </select>
                          </label>
                          <label>
                            Category
                            <select
                              value={manualTriageForm.category}
                              onChange={(event) => updateManualTriageField('category', event.target.value)}
                            >
                              {categoryOrder
                                .filter((category) => category !== 'UNCLASSIFIED')
                                .map((category) => (
                                  <option key={category} value={category}>
                                    {category}
                                  </option>
                                ))}
                            </select>
                          </label>
                        </div>
                        <label>
                          Probable cause
                          <textarea
                            value={manualTriageForm.probableCause}
                            onChange={(event) => updateManualTriageField('probableCause', event.target.value)}
                            placeholder="What the reviewer believes is causing the issue"
                            rows="3"
                          />
                        </label>
                        <label>
                          Recommended action
                          <textarea
                            value={manualTriageForm.recommendedAction}
                            onChange={(event) => updateManualTriageField('recommendedAction', event.target.value)}
                            placeholder="What operations should do next"
                            rows="3"
                          />
                        </label>
                        <div className="review-actions">
                          <button
                            className="primary-button"
                            type="submit"
                            disabled={!canSubmitManualTriage}
                          >
                            {isSubmittingManualTriage ? 'Applying' : 'Apply manual triage'}
                          </button>
                        </div>
                      </form>
                    ) : latestTriage ? (
                      <div className="review-form">
                        <label>
                          Reviewer
                          <input
                            value={reviewForm.reviewer}
                            onChange={(event) => updateReviewField('reviewer', event.target.value)}
                            disabled={!canReview || Boolean(isReviewing)}
                            placeholder="Operator"
                          />
                        </label>
                        <label>
                          Review note
                          <textarea
                            value={reviewForm.reviewNote}
                            onChange={(event) => updateReviewField('reviewNote', event.target.value)}
                            disabled={!canReview || Boolean(isReviewing)}
                            placeholder="Decision rationale"
                            rows="3"
                          />
                        </label>
                        <div className="review-actions">
                          <button
                            className="primary-button"
                            type="button"
                            onClick={() => submitReview('approve')}
                            disabled={!canSubmitReview}
                          >
                            {isReviewing === 'approve' ? 'Approving' : 'Approve'}
                          </button>
                          <button
                            className="danger-button"
                            type="button"
                            onClick={() => submitReview('reject')}
                            disabled={!canSubmitReview}
                          >
                            {isReviewing === 'reject' ? 'Rejecting' : 'Reject'}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <p className="empty-state soft-empty">Run AI triage before making a review decision.</p>
                    )}
                  </section>

                  <section className="detail-card triage-section">
                    <div className="section-heading">
                      <div>
                        <h3>{triageHeading(latestTriage)}</h3>
                        <p>{triageSubheading(latestTriage)}</p>
                      </div>
                      <button
                        className={latestTriage ? 'secondary-button compact-button' : 'primary-button compact-button'}
                        type="button"
                        onClick={runTriage}
                        disabled={isTriaging}
                      >
                        {isTriaging ? 'Running' : latestTriage ? 'Run AI triage' : 'Run triage'}
                      </button>
                    </div>
                    {latestTriage ? (
                      <div className="triage-card">
                        <div className="row-badges">
                          <Badge label={latestTriage.category} />
                          <Badge
                            label={latestTriage.severity}
                            tone={severityTone(latestTriage.severity)}
                          />
                          <Badge label={latestTriage.requiresHumanReview ? 'REVIEW REQUIRED' : 'LOW RISK'} />
                        </div>

                        <div className="triage-fields">
                          <TriageField label="Summary" value={latestTriage.summary} />
                          <TriageField label="Probable cause" value={latestTriage.probableCause} />
                          <TriageField label="Recommended action" value={latestTriage.recommendedAction} />
                          <TriageField
                            label="Confidence"
                            value={formatConfidence(latestTriage.confidenceScore)}
                          />
                          <TriageField label="Model" value={latestTriage.modelName} />
                        </div>
                      </div>
                    ) : (
                      <p className="empty-state soft-empty">Use the triage action to classify severity, category, and recommended response.</p>
                    )}
                  </section>

                  <section className="detail-card duplicates-section">
                    <div className="section-heading">
                      <div>
                        <h3>Duplicates</h3>
                        {duplicateResults.ticketId === selectedTicket.id ? (
                          <p>{duplicateCandidates.length} possible matches returned</p>
                        ) : (
                          <p>Suggested matches are advisory until marked.</p>
                        )}
                      </div>
                      <button
                        className="secondary-button compact-button"
                        type="button"
                        onClick={checkDuplicates}
                        disabled={isCheckingDuplicates}
                      >
                        {isCheckingDuplicates ? 'Checking' : 'Check'}
                      </button>
                    </div>

                    {duplicateLinks.length ? (
                      <div className="confirmed-duplicates">
                        <h4>Marked Duplicates</h4>
                        {duplicateLinks.map((link) => (
                          <button
                            className="confirmed-duplicate"
                            key={link.id}
                            type="button"
                            onClick={() => selectDuplicate(link.duplicateOfTicketId)}
                          >
                            <span>Duplicate of #{link.duplicateOfTicketId}</span>
                            <small>
                              {link.markedBy} · {formatDate(link.createdAt)}
                            </small>
                          </button>
                        ))}
                      </div>
                    ) : null}

                    <div className="suggested-duplicates">
                      <h4>Possible Duplicates</h4>
                      {duplicateResults.ticketId === selectedTicket.id ? (
                        duplicateCandidates.length ? (
                          <div className="duplicate-list">
                            {duplicateCandidates.map((candidate) => (
                              <article
                                className="duplicate-row"
                                key={candidate.ticketId}
                              >
                                <button
                                  className="duplicate-main"
                                  type="button"
                                  onClick={() => selectDuplicate(candidate.ticketId)}
                                >
                                  <span>
                                    #{candidate.ticketId} · {candidate.title}
                                  </span>
                                  <strong>{formatSimilarity(candidate.similarityScore)}</strong>
                                  <small>{candidate.reason}</small>
                                </button>
                                <button
                                  className="secondary-button compact-button"
                                  type="button"
                                  onClick={() => markDuplicate(candidate)}
                                  disabled={
                                    confirmedDuplicateIds.has(candidate.ticketId) ||
                                    isMarkingDuplicate === candidate.ticketId
                                  }
                                >
                                  {confirmedDuplicateIds.has(candidate.ticketId)
                                    ? 'Marked'
                                    : isMarkingDuplicate === candidate.ticketId
                                      ? 'Marking'
                                      : 'Mark duplicate'}
                                </button>
                              </article>
                            ))}
                          </div>
                        ) : (
                          <p className="empty-state soft-empty">No duplicate candidates found.</p>
                        )
                      ) : (
                        <p className="empty-state soft-empty">Run a duplicate check to see suggested matches.</p>
                      )}
                    </div>
                  </section>
                </div>

                <aside className="detail-rail audit-section">
                  <h3>Audit Trail</h3>
                  <ol className="audit-list">
                    {auditLogs.slice(0, 10).map((log) => (
                      <li key={log.id}>
                        <div className="audit-topline">
                          <Badge label={formatActionType(log.actionType)} tone={auditTone(log.actionType)} />
                          <small>{formatDate(log.createdAt)}</small>
                        </div>
                        <strong>{log.newValue || formatActionType(log.actionType)}</strong>
                        {log.oldValue ? <p className="audit-change">Previous: {log.oldValue}</p> : null}
                        <small>By {log.performedBy || 'system'}</small>
                      </li>
                    ))}
                  </ol>
                  {auditLogs.length === 0 ? (
                    <p className="empty-state soft-empty">No audit entries found.</p>
                  ) : null}
                </aside>
              </div>
            </>
          ) : (
            <p className="empty-state">Select a ticket to inspect details.</p>
          )}
        </div>

        <form className="panel create-panel" onSubmit={createTicket}>
          <div className="panel-header">
            <div>
              <h2>Create Ticket</h2>
              <p>Capture a new operational report</p>
            </div>
          </div>

          <label>
            Title
            <input
              value={form.title}
              onChange={(event) => updateField('title', event.target.value)}
              placeholder="Map layer loading slowly"
            />
          </label>

          <label>
            Description
            <textarea
              value={form.description}
              onChange={(event) => updateField('description', event.target.value)}
              placeholder="Several users report high latency during morning operations."
              rows="5"
            />
          </label>

          <label>
            Source system
            <input
              value={form.sourceSystem}
              onChange={(event) => updateField('sourceSystem', event.target.value)}
            />
          </label>

          <label>
            Affected service
            <input
              value={form.affectedService}
              onChange={(event) => updateField('affectedService', event.target.value)}
              placeholder="NinJo"
            />
          </label>

          <button className="primary-button" type="submit" disabled={!canCreateTicket || isSubmitting}>
            {isSubmitting ? 'Creating ticket' : 'Create ticket'}
          </button>
        </form>
      </section>

      <section className="panel distribution-panel">
        <Distribution title="Severity" order={severityOrder} values={dashboard.ticketsBySeverity} />
        <Distribution title="Category" order={categoryOrder} values={dashboard.ticketsByCategory} />
      </section>
    </main>
  )
}

function Metric({ label, value, intent }) {
  return (
    <article className={`metric ${intent === 'danger' ? 'is-danger' : ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function Fact({ label, value }) {
  return (
    <div className="fact">
      <span>{label}</span>
      <strong>{value || 'Unset'}</strong>
    </div>
  )
}

function TriageField({ label, value }) {
  return (
    <div className="triage-field">
      <span>{label}</span>
      <strong>{value || 'Unset'}</strong>
    </div>
  )
}

function Badge({ label, tone }) {
  return <span className={`badge ${tone ? `badge-${tone}` : ''}`}>{label}</span>
}

function WorkflowStep({ label, status }) {
  return (
    <div className={`workflow-step is-${status}`}>
      <span aria-hidden="true" />
      <strong>{label}</strong>
    </div>
  )
}

function workflowMessage(ticket, latestTriage, duplicateLinks) {
  if (duplicateLinks.length) {
    return `Marked duplicate relationship recorded for ticket #${ticket.id}.`
  }

  if (ticket.status === 'OPEN') {
    return 'Run AI triage to prepare the ticket for review.'
  }

  if (ticket.status === 'PENDING_REVIEW' && latestTriage) {
    return 'Review the AI recommendation, then approve or reject it.'
  }

  if (ticket.status === 'PENDING_REVIEW') {
    return 'Waiting for a triage result before review.'
  }

  if (ticket.status === 'APPROVED') {
    return 'AI recommendation approved and applied.'
  }

  if (ticket.status === 'REJECTED') {
    return 'AI recommendation rejected. Apply manual triage to continue.'
  }

  if (ticket.status === 'IN_PROGRESS') {
    return 'Ticket is triaged and ready for operations work.'
  }

  return `Ticket is currently ${ticket.status}.`
}

function decisionMessage(ticket, latestTriage) {
  if (!latestTriage) {
    return 'A decision becomes available after AI triage.'
  }

  if (ticket.status === 'PENDING_REVIEW') {
    return 'Confirm whether to apply the recommendation.'
  }

  if (ticket.status === 'APPROVED') {
    return 'Recommendation approved and applied.'
  }

  if (ticket.status === 'REJECTED') {
    return 'Provide the corrected human triage.'
  }

  if (ticket.status === 'IN_PROGRESS') {
    return 'Manual triage has been applied.'
  }

  return `Review unavailable while ticket is ${ticket.status}.`
}

function decisionStepStatus(ticket, canReview) {
  if (ticket.status === 'APPROVED' || ticket.status === 'REJECTED' || ticket.status === 'IN_PROGRESS') {
    return 'complete'
  }

  return canReview ? 'current' : 'pending'
}

function triageHeading(latestTriage) {
  if (latestTriage?.modelName === 'manual') {
    return 'Manual Triage'
  }

  return 'AI Triage'
}

function triageSubheading(latestTriage) {
  if (!latestTriage) {
    return 'No recommendation generated yet'
  }

  if (latestTriage.modelName === 'manual') {
    return 'Human correction applied after rejection'
  }

  return 'Latest recommendation'
}

function formatActionType(actionType) {
  return actionType
    ? actionType
        .split('_')
        .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
        .join(' ')
    : 'Audit event'
}

function auditTone(actionType) {
  if (actionType === 'TRIAGE_REJECTED' || actionType === 'AI_TRIAGE_FAILED') {
    return 'danger'
  }

  if (actionType === 'TRIAGE_APPROVED' || actionType === 'AI_TRIAGE_COMPLETED') {
    return 'success'
  }

  if (actionType === 'DUPLICATE_CHECKED' || actionType === 'TRIAGE_REQUESTED') {
    return 'warning'
  }

  return 'neutral'
}

function Distribution({ title, order, values = {} }) {
  const rows = order.map((key) => ({ key, value: values[key] ?? 0 })).filter((row) => row.value > 0)

  return (
    <div>
      <h3>{title}</h3>
      {rows.length ? (
        <div className="distribution-list">
          {rows.map((row) => (
            <div className="distribution-row" key={row.key}>
              <span>{row.key}</span>
              <strong>{row.value}</strong>
            </div>
          ))}
        </div>
      ) : (
        <p className="empty-state">No {title.toLowerCase()} data yet.</p>
      )}
    </div>
  )
}

function getFilterCounts(tickets) {
  return tickets.reduce(
    (counts, ticket) => {
      counts.ALL += 1
      if (matchesTicketFilter(ticket, 'ATTENTION')) {
        counts.ATTENTION += 1
      }
      if (counts[ticket.status] !== undefined) {
        counts[ticket.status] += 1
      }
      return counts
    },
    {
      ATTENTION: 0,
      ALL: 0,
      OPEN: 0,
      PENDING_REVIEW: 0,
      APPROVED: 0,
      REJECTED: 0,
    },
  )
}

function readStoredSelectedTicketId() {
  const storedId = window.localStorage.getItem(selectedTicketStorageKey)
  return storedId ? Number(storedId) : null
}

function storeSelectedTicketId(ticketId) {
  if (ticketId) {
    window.localStorage.setItem(selectedTicketStorageKey, String(ticketId))
  } else {
    window.localStorage.removeItem(selectedTicketStorageKey)
  }
}

function resolveSelectedTicketId(tickets, preferredTicketId) {
  if (preferredTicketId && tickets.some((ticket) => ticket.id === preferredTicketId)) {
    return preferredTicketId
  }

  const sortedTickets = sortTickets(tickets, 'NEWEST')
  return sortedTickets[0]?.id ?? null
}

function matchesTicketFilter(ticket, filter) {
  if (filter === 'ALL') {
    return true
  }

  if (filter === 'ATTENTION') {
    return (
      ticket.status === 'PENDING_REVIEW' ||
      (ticket.status === 'OPEN' && (ticket.severity === 'CRITICAL' || ticket.severity === 'HIGH'))
    )
  }

  return ticket.status === filter
}

function matchesTicketSearch(ticket, search) {
  const normalizedSearch = search.trim().toLowerCase()

  if (!normalizedSearch) {
    return true
  }

  return [
    ticket.title,
    ticket.description,
    ticket.affectedService,
    ticket.sourceSystem,
    ticket.status,
    ticket.severity,
    ticket.category,
    ticket.id?.toString(),
  ]
    .filter(Boolean)
    .some((value) => value.toLowerCase().includes(normalizedSearch))
}

function sortTickets(tickets, sort) {
  return [...tickets].sort((left, right) => {
    if (sort === 'OLDEST') {
      return timestamp(left.createdAt) - timestamp(right.createdAt)
    }

    if (sort === 'SEVERITY') {
      return compareRank(severityRank, left.severity, right.severity) || newestFirst(left, right)
    }

    if (sort === 'STATUS') {
      return compareRank(statusRank, left.status, right.status) || newestFirst(left, right)
    }

    return newestFirst(left, right)
  })
}

function compareRank(rank, left, right) {
  return (rank[left] ?? Number.MAX_SAFE_INTEGER) - (rank[right] ?? Number.MAX_SAFE_INTEGER)
}

function newestFirst(left, right) {
  return timestamp(right.createdAt) - timestamp(left.createdAt)
}

function timestamp(value) {
  return value ? new Date(value).getTime() : 0
}

function severityTone(severity) {
  if (severity === 'CRITICAL' || severity === 'HIGH') {
    return 'danger'
  }
  if (severity === 'MEDIUM') {
    return 'warning'
  }
  return 'neutral'
}

function formatDate(value) {
  if (!value) {
    return 'No timestamp'
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatConfidence(value) {
  if (value === null || value === undefined) {
    return 'Unset'
  }

  return `${Math.round(Number(value) * 100)}%`
}

function formatSimilarity(value) {
  return `${Math.round(Number(value) * 100)}%`
}

export default App
